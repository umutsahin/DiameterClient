package com.optiva;

import com.optiva.console.Console;
import com.optiva.flows.DiameterFlow;
import com.optiva.flows.DiameterIMS;
import com.optiva.flows.DiameterPS;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.optiva.OpenTelemetryConfig.messageLatencyHistogram;
import static com.optiva.OpenTelemetryConfig.tracer;

public class MessageScheduler {
    private final Vertx vertx;
    private final DiameterClientVerticle clientVerticle;
    private final Queue<DiameterFlow> messageQueue = new LinkedList<>();
    private long timerId = -1;
    private final AtomicInteger activeFlowsCounter; // To be passed from MainVerticle
    private final Supplier<DiameterFlow> flowGenerator;
    private final Random random = new Random(); // For generating flow parameters
    private final AtomicReference<String> chargingFlowName = new AtomicReference<>("ims");
    private final AtomicInteger ratingGroup = new AtomicInteger(10);
    private final AtomicInteger messageCount = new AtomicInteger(4);

    public MessageScheduler(Vertx vertx, DiameterClientVerticle clientVerticle, AtomicInteger activeFlowsCounter) {
        this.vertx = vertx;
        this.clientVerticle = clientVerticle;
        this.activeFlowsCounter = activeFlowsCounter;
        this.flowGenerator = () -> {
            String msisdn = "447400000" + String.format("%04d", random.nextInt(1000));
            return switch (chargingFlowName.get()) {
                case "ims" -> new DiameterIMS(msisdn, ratingGroup.get(), messageCount.get());
                case "ps" -> new DiameterPS(msisdn, ratingGroup.get(), messageCount.get());
                default -> throw new IllegalStateException("Unexpected value: " + chargingFlowName.get());
            };
        };
    }

    public void setRps(int rps) {
        Console.log("MessageScheduler: Setting RPS to " + rps);
        if (timerId != -1) {
            vertx.cancelTimer(timerId);
            timerId = -1;
        }

        if (rps > 0) {
            long interval = Math.max(1, 1000 / rps);
            timerId = vertx.setPeriodic(interval, id -> processQueue());
            Console.log("MessageScheduler: Timer started with interval " + interval + "ms for RPS " + rps);
        } else {
            Console.log("MessageScheduler: RPS set to 0, timer stopped.");
        }
    }

    public void scheduleFlow() {
        activeFlowsCounter.incrementAndGet();
        DiameterFlow flow = flowGenerator.get();

        Console.log("MessageScheduler: Scheduling " + flow + ". Active flows: " + activeFlowsCounter.get());
        synchronized (messageQueue) {
            messageQueue.add(flow);
        }
    }

    private void processQueue() {
        DiameterFlow flow = getDiameterFlow();

        Span span = createSpan(flow, "Process Message");

        try (Scope ignored = span.makeCurrent()) {
            Buffer messageToSend = flow.getNextMessage();

            if (messageToSend == null) {
                Console.debug("MessageScheduler: Flow "
                              + flow.getKey()
                              + " yielded no message or is complete. Completing it.");
                completeFlow(flow);
                closeSpan(span, StatusCode.OK, "Flow completed without sending message", null);
                return;
            }

            long startTimeNanos = System.nanoTime();

            clientVerticle.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
                try (Scope ignored1 = span.makeCurrent()) { // Re-activate span in callback
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    messageLatencyHistogram.record(durationNanos / 1_000_000.0);

                    Console.debug("MessageScheduler: Received response for flow " + flow.getKey());
                    flow.processResponse(responseMessage);
                    if (flow.isInProgress()) {
                        Console.debug("MessageScheduler: Flow " + flow.getKey() + " has next message. Re-scheduling.");
                        synchronized (messageQueue) {
                            messageQueue.add(flow);
                        }
                    } else {
                        Console.debug("MessageScheduler: "
                                      + flow
                                      + " completed or has no further messages after response.");
                        completeFlow(flow);
                    }
                } finally {
                    closeSpan(span, StatusCode.OK, "Completed message flow", null);
                }
            }).onFailure(err -> {
                try (Scope ignored1 = span.makeCurrent()) { // Re-activate span in callback
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    messageLatencyHistogram.record(durationNanos / 1_000_000.0);
                    Console.error("MessageScheduler: Failed to send message for " + flow + ": " + err.getMessage());
                    flow.terminateFlow();
                    completeFlow(flow);
                } finally {
                    closeSpan(span, StatusCode.ERROR, "Failed to send message: " + err.getMessage(), err);
                }
            });
        } catch (Exception e) {
            closeSpan(span, StatusCode.ERROR, "Exception in processQueue: " + e.getMessage(), e);
            Console.error("Synchronous exception in processQueue for flow " + flow.getKey() + ": " + e.getMessage());
        }
    }

    public void singleFlow() {
        singleFlow(flowGenerator.get());
    }

    private void singleFlow(DiameterFlow flow) {
        Span span = createSpan(flow, "Process Single Message");

        try (Scope ignored = span.makeCurrent()) {
            Buffer messageToSend = flow.getNextMessage();
            if (messageToSend == null) {
                Console.debug("MessageScheduler: singleFlow " + flow.getKey() + " yielded no message or is complete.");
                closeSpan(span, StatusCode.OK, "Flow completed without sending message", null);
                return;
            }
            // Optionally: span.setAttribute("message.type", flow.getMessageType());

            long startTimeNanos = System.nanoTime();

            clientVerticle.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
                long durationNanos = System.nanoTime() - startTimeNanos;
                messageLatencyHistogram.record(durationNanos / 1_000_000.0);
                try (Scope ignored1 = span.makeCurrent()) {
                    Console.debug("MessageScheduler: Received response for flow " + flow.getKey());
                } finally {
                    closeSpan(span, StatusCode.OK, "Response message received", null);
                }
                flow.processResponse(responseMessage);
                if (flow.isInProgress()) {
                    Console.log("MessageScheduler: " + flow + " has next message.");
                    singleFlow(flow);
                } else {
                    Console.log("MessageScheduler: " + flow + " completed (singleFlow).");
                }
            }).onFailure(err -> {
                try (Scope ignored1 = span.makeCurrent()) {
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    messageLatencyHistogram.record(durationNanos / 1_000_000.0);
                    Console.error("MessageScheduler: Failed to send message for "
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
            Console.error("Synchronous exception in singleFlow for flow " + flow.getKey() + ": " + e.getMessage());
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

        if (flow == null) {
            activeFlowsCounter.incrementAndGet();
            flow = flowGenerator.get();
        }
        return flow;
    }

    private void completeFlow(DiameterFlow flow) {
        int remainingActive = activeFlowsCounter.decrementAndGet();
        Console.debug("MessageScheduler: Flow " + flow.getKey() + " marked complete. Active flows: " + remainingActive);
    }

    public void flow(String flowName) {
        chargingFlowName.set(flowName);
    }

    public boolean isNotFlowSet() {
        return chargingFlowName.get() == null;
    }

    public void setRatingGroup(int rg) {
        ratingGroup.set(rg);
    }

    public void setMessageCount(int mc) {
        messageCount.set(mc);
    }
}
