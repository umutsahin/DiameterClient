package com.optiva;

import com.optiva.OpenTelemetryConfig;
import com.optiva.console.Console;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import com.optiva.flows.DiameterFlow;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class MessageScheduler {
    private final Vertx vertx;
    private final DiameterClientVerticle clientVerticle;
    private final Queue<DiameterFlow> messageQueue = new LinkedList<>();
    private long timerId = -1;
    private final AtomicInteger activeFlowsCounter; // To be passed from MainVerticle
    private final Supplier<DiameterFlow> flowGenerator;

    public MessageScheduler(Vertx vertx,
                            DiameterClientVerticle clientVerticle,
                            AtomicInteger activeFlowsCounter,
                            Supplier<DiameterFlow> flowGenerator) {
        this.vertx = vertx;
        this.clientVerticle = clientVerticle;
        this.activeFlowsCounter = activeFlowsCounter;
        this.flowGenerator = flowGenerator;
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
        DiameterFlow flow;
        flow = getDiameterFlow();

        Span span = OpenTelemetryConfig.getTracer().spanBuilder("Process Message")
                .setAttribute("flow.key", flow.getKey())
                .startSpan();

        // Make the span current for operations within this method
        try (Scope scope = span.makeCurrent()) {
            Buffer messageToSend = flow.getNextMessage();

            if (messageToSend == null) {
                Console.debug("MessageScheduler: Flow " + flow.getKey() + " yielded no message or is complete. Completing it.");
                completeFlow(flow);
                span.setStatus(StatusCode.OK, "Flow completed without sending message");
                span.end(); // End span here
                return;
            }
            // Optionally: flow.getMessageType() or similar if you want to add it as an attribute
            // span.setAttribute("message.type", flow.getMessageType());


            long startTimeNanos = System.nanoTime();

            clientVerticle.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
                try (Scope s = span.makeCurrent()) { // Re-activate span in callback
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    OpenTelemetryConfig.getMessageLatencyHistogram().record(durationNanos / 1_000_000.0);

                    Console.debug("MessageScheduler: Received response for flow " + flow.getKey());
                    flow.processResponse(responseMessage);
                    if (flow.isInProgress()) {
                        Console.debug("MessageScheduler: Flow " + flow.getKey() + " has next message. Re-scheduling.");
                        synchronized (messageQueue) {
                            messageQueue.add(flow);
                        }
                    } else {
                        Console.debug("MessageScheduler: " + flow + " completed or has no further messages after response.");
                        completeFlow(flow);
                    }
                    span.setStatus(StatusCode.OK);
                } finally {
                    span.end(); // End span in success callback
                }
            }).onFailure(err -> {
                try (Scope s = span.makeCurrent()) { // Re-activate span in callback
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    OpenTelemetryConfig.getMessageLatencyHistogram().record(durationNanos / 1_000_000.0);

                    Console.error("MessageScheduler: Failed to send message for " + flow + ": " + err.getMessage());
                    span.setStatus(StatusCode.ERROR, "Failed to send message: " + err.getMessage());
                    span.recordException(err);
                    flow.terminateFlow();
                    completeFlow(flow);
                } finally {
                    span.end(); // End span in failure callback
                }
            });
        } catch (Exception e) { // Catch synchronous exceptions
            span.setStatus(StatusCode.ERROR, "Exception in processQueue: " + e.getMessage());
            span.recordException(e);
            span.end();
            // Depending on desired behavior, you might want to rethrow or handle differently
            // For now, just logging via span and ending it.
             Console.error("Synchronous exception in processQueue for flow " + flow.getKey() + ": " + e.getMessage());
        }
    }

    public void singleFlow(DiameterFlow flow) {
        Span span = OpenTelemetryConfig.getTracer().spanBuilder("Process Single Message")
            .setAttribute("flow.key", flow.getKey())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Buffer messageToSend = flow.getNextMessage();
            if (messageToSend == null) {
                Console.debug("MessageScheduler: singleFlow " + flow.getKey() + " yielded no message or is complete.");
                span.setStatus(StatusCode.OK, "Flow completed without sending message");
                span.end();
                return;
            }
            // Optionally: span.setAttribute("message.type", flow.getMessageType());


            long startTimeNanos = System.nanoTime();

            clientVerticle.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
                try(Scope s = span.makeCurrent()) {
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    OpenTelemetryConfig.getMessageLatencyHistogram().record(durationNanos / 1_000_000.0);

                    Console.debug("MessageScheduler: Received response for flow " + flow.getKey());
                    flow.processResponse(responseMessage);
                    if (flow.isInProgress()) {
                        Console.debug("MessageScheduler: Flow " + flow.getKey() + " has next message. Re-scheduling for singleFlow.");
                        // Recursive call, new span will be created for the next message in singleFlow.
                        // Current span for *this* message ends after this callback.
                        singleFlow(flow);
                    } else {
                        Console.debug("MessageScheduler: " + flow + " completed (singleFlow).");
                    }
                    span.setStatus(StatusCode.OK);
                } finally {
                    span.end(); // End span for this message delivery
                }
            }).onFailure(err -> {
                try (Scope s = span.makeCurrent()) {
                    long durationNanos = System.nanoTime() - startTimeNanos;
                    OpenTelemetryConfig.getMessageLatencyHistogram().record(durationNanos / 1_000_000.0);

                    Console.error("MessageScheduler: Failed to send message for " + flow + " in singleFlow: " + err.getMessage());
                    span.setStatus(StatusCode.ERROR, "Failed to send message in singleFlow: " + err.getMessage());
                    span.recordException(err);
                    flow.terminateFlow();
                } finally {
                    span.end(); // End span for this message delivery attempt
                }
            });
        } catch (Exception e) { // Catch synchronous exceptions
            span.setStatus(StatusCode.ERROR, "Exception in singleFlow: " + e.getMessage());
            span.recordException(e);
            span.end();
            Console.error("Synchronous exception in singleFlow for flow " + flow.getKey() + ": " + e.getMessage());
        }
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
}
