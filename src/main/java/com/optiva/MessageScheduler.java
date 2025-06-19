package com.optiva;

import com.optiva.console.Console;
import com.optiva.flows.DiameterChargingFlow;
import com.optiva.flows.DiameterFlow;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.optiva.handler.CommandHandler.EB_COMMAND_ACTIVE_FLOWS;
import static com.optiva.handler.CommandHandler.EB_COMMAND_RPS;
import static com.optiva.handler.CommandHandler.EB_COMMAND_SINGLE;
import static com.optiva.observability.OpenTelemetryConfig.messageLatencyHistogram;
import static com.optiva.observability.OpenTelemetryConfig.tracer;

public class MessageScheduler extends AbstractVerticle {
    private String logPrefix;
    private DiameterClient diameterClient;
    private String diameterHost;
    private int diameterPort;
    private final Queue<DiameterFlow> messageQueue = new LinkedList<>();
    private long timerId = -1;
    private final AtomicInteger activeFlows = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private String chargingFlowName;
    private int ratingGroup;
    private int messageCount;

    @Override
    public void start(Promise<Void> startPromise) {
        logPrefix = MessageScheduler.class.getSimpleName() + "(" + deploymentID() + "): ";
        JsonObject config = config();
        this.diameterHost = config.getString("diameterHost", "localhost");
        this.diameterPort = config.getInteger("diameterPort", 3868);

        chargingFlowName = config.getString("flowName", "ims-moc");
        ratingGroup = config.getInteger("ratingGroup", 10);
        messageCount = config.getInteger("messageCount", 4);

        this.diameterClient = new DiameterClient(vertx, diameterHost, diameterPort);
        this.diameterClient.connect().onComplete(res -> {
            if (res.succeeded()) {
                Console.log(logPrefix
                            + "DiameterClient connected successfully to "
                            + diameterHost
                            + ":"
                            + diameterPort);
                startPromise.complete();
            } else {
                Console.error(logPrefix
                              + "DiameterClient failed to connect to "
                              + diameterHost
                              + ":"
                              + diameterPort
                              + ": "
                              + res.cause());
                startPromise.fail(res.cause());
            }
        });

        vertx.eventBus().<JsonObject>consumer(EB_COMMAND_SINGLE, message -> this.singleFlow());

        vertx.eventBus().<JsonObject>consumer(EB_COMMAND_RPS, message -> setRps(message.body().getInteger("value")));
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        Console.log(logPrefix + "stop called.");
        shutdown().onComplete(ar -> {
            if (ar.succeeded()) {
                Console.log(logPrefix + "shutdown completed.");
                stopPromise.complete();
            } else {
                Console.error(logPrefix + "shutdown failed: " + ar.cause());
                stopPromise.fail(ar.cause());
            }
        });
    }

    public Future<Void> setRps(int rps) {
        Console.debug(logPrefix + "Setting RPS to " + rps);
        if (rps > 0) {
            if (timerId != -1) {
                vertx.cancelTimer(timerId);
                timerId = -1;
            }
            running.set(true);
            long interval = Math.max(1, 1000 / rps);
            timerId = vertx.setPeriodic(interval, id -> processQueue());
            Console.debug(logPrefix + "Timer started with interval " + interval + "ms for RPS " + rps);
            return Future.succeededFuture();
        } else {
            running.set(false);
            Promise<Void> promise = Promise.promise();
            vertx.setPeriodic(1000, checkId -> {
                int currentActive = activeFlows.get();
                if (currentActive == 0) {
                    vertx.cancelTimer(checkId);
                    if (timerId != -1) {
                        vertx.cancelTimer(timerId);
                        timerId = -1;
                    }
                    Console.log("All active flows completed.");
                    promise.complete();
                } else {
                    Console.log("Waiting for " + currentActive + " active flows to complete...");
                }
            });
            Console.log(logPrefix + "RPS set to 0, timer stopped.");
            return promise.future();
        }
    }

    private void processQueue() {
        DiameterFlow flow = getDiameterFlow();
        if (flow == null) {
            return;
        }

        Span span = createSpan(flow, "Process Message");

        try (Scope ignored = span.makeCurrent()) {
            Buffer messageToSend = flow.getNextMessage();

            if (messageToSend == null) {
                Console.error(logPrefix
                              + "Flow "
                              + flow.getKey()
                              + " yielded no message or is complete. Completing it.");
                completeFlow(flow);
                closeSpan(span, StatusCode.OK, "Flow completed without sending message", null);
                return;
            }

            long startTimeNanos = System.nanoTime();

            this.diameterClient.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
                try (Scope ignored1 = span.makeCurrent()) { // Re-activate span in callback
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    messageLatencyHistogram.record(durationNanos / 1_000_000.0);

                    Console.debug(logPrefix + "Received response for flow " + flow.getKey());
                    flow.processResponse(responseMessage);
                    if (flow.isInProgress()) {
                        Console.debug(logPrefix + "Flow " + flow.getKey() + " has next message. Re-scheduling.");
                        synchronized (messageQueue) {
                            messageQueue.add(flow);
                        }
                    } else {
                        Console.debug(logPrefix + flow + " completed or has no further messages after response.");
                        completeFlow(flow);
                    }
                } finally {
                    closeSpan(span, StatusCode.OK, "Completed message flow", null);
                }
            }).onFailure(err -> {
                try (Scope ignored1 = span.makeCurrent()) { // Re-activate span in callback
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    messageLatencyHistogram.record(durationNanos / 1_000_000.0);
                    Console.error(logPrefix + "Failed to send message for " + flow + ": " + err.getMessage());
                    flow.terminateFlow();
                    completeFlow(flow);
                } finally {
                    closeSpan(span, StatusCode.ERROR, "Failed to send message: " + err.getMessage(), err);
                }
            });
        } catch (Exception e) {
            closeSpan(span, StatusCode.ERROR, "Exception in processQueue: " + e.getMessage(), e);
            Console.error("Synchronous exception in processQueue for flow "
                          + flow.getKey()
                          + " in "
                          + deploymentID()
                          + ": "
                          + e.getMessage());
        }
    }

    public void singleFlow() {
        Console.log(logPrefix + "Starting single flow via event bus...");
        singleFlow(DiameterChargingFlow.newInstance(chargingFlowName, ratingGroup, messageCount));
    }

    private void singleFlow(DiameterFlow flow) {
        Span span = createSpan(flow, "Process Single Message");

        try (Scope ignored = span.makeCurrent()) {
            Buffer messageToSend = flow.getNextMessage();
            if (messageToSend == null) {
                Console.debug(logPrefix + "singleFlow " + flow.getKey() + " yielded no message or is complete.");
                closeSpan(span, StatusCode.OK, "Flow completed without sending message", null);
                return;
            }
            long startTimeNanos = System.nanoTime();

            this.diameterClient.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
                long durationNanos = System.nanoTime() - startTimeNanos;
                messageLatencyHistogram.record(durationNanos / 1_000_000.0);
                try (Scope ignored1 = span.makeCurrent()) {
                    Console.debug(logPrefix + "Received response for flow " + flow.getKey());
                } finally {
                    closeSpan(span, StatusCode.OK, "Response message received", null);
                }
                flow.processResponse(responseMessage);
                if (flow.isInProgress()) {
                    Console.log(logPrefix + flow + " has next message.");
                    singleFlow(flow);
                } else {
                    Console.log(logPrefix + flow + " completed.");
                }
            }).onFailure(err -> {
                try (Scope ignored1 = span.makeCurrent()) {
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    messageLatencyHistogram.record(durationNanos / 1_000_000.0);
                    Console.error(logPrefix
                                  + "Failed to send message for "
                                  + flow
                                  + " in singleFlow: "
                                  + err.getMessage());
                    flow.terminateFlow();
                } finally {
                    closeSpan(span, StatusCode.ERROR, "Failed to send message in singleFlow: " + err.getMessage(), err);
                    span.end(); // End span for this message delivery attempt
                }
            });
        } catch (Exception e) {
            closeSpan(span, StatusCode.ERROR, "Exception in singleFlow: " + e.getMessage(), e);
            Console.error("Synchronous exception in singleFlow for flow "
                          + flow.getKey()
                          + " in "
                          + deploymentID()
                          + ": "
                          + e.getMessage());
        }
    }

    private static Span createSpan(DiameterFlow flow, String spanName) {
        return tracer.spanBuilder(spanName).setAttribute("flow.key", flow.getKey()).startSpan();
    }

    private static void closeSpan(Span span, StatusCode status, String description, Throwable e) {
        span.setStatus(status, description);
        if (e != null) {
            span.recordException(e);
        }
        span.end();
    }

    private DiameterFlow getDiameterFlow() {
        DiameterFlow flow;
        synchronized (messageQueue) {
            flow = messageQueue.poll();
        }

        if (flow == null && running.get()) {
            int currentActive = activeFlows.incrementAndGet();
            vertx.eventBus()
                    .publish(EB_COMMAND_ACTIVE_FLOWS,
                             new JsonObject().put("schedulerId", deploymentID()).put("activeFlows", currentActive));
            flow = DiameterChargingFlow.newInstance(chargingFlowName, ratingGroup, messageCount);
            Console.debug(logPrefix + "Starting new flow: " + flow.getKey() + ", active flows: " + currentActive);
        }
        return flow;
    }

    private void completeFlow(DiameterFlow flow) {
        int currentActive = activeFlows.decrementAndGet();
        // Publish active flow update
        vertx.eventBus()
                .publish(EB_COMMAND_ACTIVE_FLOWS,
                         new JsonObject().put("schedulerId", deploymentID()).put("activeFlows", currentActive));
        Console.debug(logPrefix + "Flow " + flow.getKey() + " marked complete. Active flows: " + currentActive);
    }

    private Future<Void> shutdown() {
        Console.log(logPrefix + "Shutting down...");
        setRps(0).andThen(event -> diameterClient.close());
        return Future.succeededFuture();
    }
}
