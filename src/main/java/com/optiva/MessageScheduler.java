package com.optiva;

import com.optiva.console.Console;
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
        Buffer messageToSend = flow.getNextMessage();

        if (messageToSend == null) {
            // Flow might have been completed by a previous operation or yielded no message initially.
            Console.debug("MessageScheduler: Flow "
                          + flow.getKey()
                          + " yielded no message initially or is already complete. Completing it.");
            completeFlow(flow); // Ensure activeFlows is decremented if it was incremented by scheduleFlow
            return;
        }

        clientVerticle.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
            Console.debug("MessageScheduler: Received response for flow " + flow.getKey());
            flow.processResponse(responseMessage); // Let the flow update its state based on the response
            if (flow.isInProgress()) {
                Console.debug("MessageScheduler: Flow " + flow.getKey() + " has next message. Re-scheduling.");
                synchronized (messageQueue) {
                    messageQueue.add(flow); // Re-queue the same flow for its next message
                }
            } else {
                Console.debug("MessageScheduler: " + flow + " completed or has no further messages after response.");
                completeFlow(flow);
            }
        }).onFailure(err -> {
            Console.error("MessageScheduler: Failed to send message for " + flow + ": " + err.getMessage());
            flow.terminateFlow();
            completeFlow(flow);
        });
    }

    public void singleFlow(DiameterFlow flow) {
        Buffer messageToSend = flow.getNextMessage();
        clientVerticle.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
            Console.debug("MessageScheduler: Received response for flow " + flow.getKey());
            flow.processResponse(responseMessage); // Let the flow update its state based on the response
            if (flow.isInProgress()) {
                Console.debug("MessageScheduler: Flow " + flow.getKey() + " has next message. Re-scheduling.");
                singleFlow(flow);
            } else {
                Console.debug("MessageScheduler: " + flow + " completed or has no further messages after response.");
            }
        }).onComplete(e -> Console.log(flow + " completed...")).onFailure(err -> {
            Console.error("MessageScheduler: Failed to send message for " + flow + ": " + err.getMessage());
            flow.terminateFlow();
        });
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
