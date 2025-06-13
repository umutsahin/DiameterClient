package com.optiva;

import com.optiva.flows.DiameterFlow;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.Handler;
import com.optiva.charging.openapi.diameter.DiameterMessage; // Assuming this is the correct DiameterMessage class

import java.util.Queue;
import java.util.LinkedList; // Using LinkedList for now, can be changed if concurrency becomes an issue
import java.util.concurrent.atomic.AtomicInteger;

public class MessageScheduler {

    private static class MessageRequest {
        final DiameterFlow flow;
        // Initially, let's assume a flow can tell us its current message to send
        // and how to process a response to prepare the next one.
        // This simplifies the MessageRequest for now.

        MessageRequest(DiameterFlow flow) {
            this.flow = flow;
        }
    }

    private final Vertx vertx;
    private final DiameterClientVerticle clientVerticle;
    private final Queue<MessageRequest> messageQueue = new LinkedList<>();
    private long timerId = -1;
    private final AtomicInteger currentRps = new AtomicInteger(0);
    private final AtomicInteger activeFlowsCounter; // To be passed from MainVerticle

    public MessageScheduler(Vertx vertx, DiameterClientVerticle clientVerticle, AtomicInteger activeFlowsCounter) {
        this.vertx = vertx;
        this.clientVerticle = clientVerticle;
        this.activeFlowsCounter = activeFlowsCounter; // For tracking active flows
    }

    public void setRps(int rps) {
        Console.log("MessageScheduler: Setting RPS to " + rps);
        if (timerId != -1) {
            vertx.cancelTimer(timerId);
            timerId = -1;
        }

        currentRps.set(rps);

        if (rps > 0) {
            long interval = Math.max(1, 1000 / rps);
            timerId = vertx.setPeriodic(interval, id -> processQueue());
            Console.log("MessageScheduler: Timer started with interval " + interval + "ms for RPS " + rps);
        } else {
            Console.log("MessageScheduler: RPS set to 0, timer stopped.");
        }
    }

    public void scheduleFlow(DiameterFlow flow) {
        if (flow == null) {
            Console.warn("MessageScheduler: Cannot schedule a null flow.");
            return;
        }
        // When a flow is scheduled, it implies it becomes "active" in terms of message generation.
        // The activeFlowsCounter should reflect flows that are currently being processed by the scheduler.
        activeFlowsCounter.incrementAndGet();
        Console.debug("MessageScheduler: Scheduling flow " + flow.getKey() + ". Queue size: " + messageQueue.size() + ". Active flows: " + activeFlowsCounter.get());
        synchronized (messageQueue) { // Synchronize access if using LinkedList from different threads, though Vert.x handlers usually run on event loop
            messageQueue.add(new MessageRequest(flow));
        }
    }

    private void processQueue() {
        MessageRequest request;
        synchronized (messageQueue) {
            request = messageQueue.poll();
        }

        if (request == null) {
            // Queue is empty, nothing to do for this tick
            return;
        }

        DiameterFlow flow = request.flow;
        Buffer messageToSend = flow.getNextMessage();

        if (messageToSend == null) {
            // Flow might have been completed by a previous operation or yielded no message initially.
            Console.debug("MessageScheduler: Flow " + flow.getKey() + " yielded no message initially or is already complete. Completing it.");
            completeFlow(flow); // Ensure activeFlows is decremented if it was incremented by scheduleFlow
            return;
        }
        // The +1 is because we polled one item before checking the size.
        Console.debug("MessageScheduler: Sending message for flow " + flow.getKey() + ". Approx. queue size: " + (messageQueue.size() + 1));

        clientVerticle.sendWithResponseHandler(messageToSend, flow.getKey(), responseMessage -> {
            Console.debug("MessageScheduler: Received response for flow " + flow.getKey());
            flow.processResponse(responseMessage); // Let the flow update its state based on the response

            Buffer nextMessage = flow.getNextMessage(); // Check if the flow has another message to send

            if (nextMessage != null) {
                Console.debug("MessageScheduler: Flow " + flow.getKey() + " has next message. Re-scheduling.");
                synchronized (messageQueue) {
                    messageQueue.add(new MessageRequest(flow)); // Re-queue the same flow for its next message
                }
            } else {
                // Flow is complete (successfully or due to response indicating end) or has no further messages
                Console.debug("MessageScheduler: Flow " + flow.getKey() + " completed or has no further messages after response.");
                completeFlow(flow);
            }
        }).onFailure(err -> {
            Console.error("MessageScheduler: Failed to send message for flow " + flow.getKey() + ": " + err.getMessage());
            flow.terminate(); // Inform the flow it's being terminated due to send failure
            completeFlow(flow); // Decrement active count and perform cleanup
        });
    }

    private void completeFlow(DiameterFlow flow) {
        // This method is called when a flow finishes (either successfully or due to error/no more messages)
        // It was previously incremented when scheduled.
        int remainingActive = activeFlowsCounter.decrementAndGet();
        Console.debug("MessageScheduler: Flow " + flow.getKey() + " marked complete. Active flows: " + remainingActive);
        // Additional cleanup for the flow if needed can be done here.
    }


    // Other potential methods:
    // - stop() to cancel timer and clear queue
    // - statistics (e.g., queue size, messages sent)
}
