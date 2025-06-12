package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterMessage; // Assuming this class can be constructed from a Buffer or byte[]
import com.optiva.flows.DiameterFlow;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import java.nio.ByteBuffer; // Added for parsing placeholder
import java.util.concurrent.atomic.AtomicInteger;

public class DiameterFlowExecutor {

    private final Vertx vertx;
    private final DiameterClientVerticle clientVerticle;
    private final AtomicInteger activeFlowsCounter; // Provided by MainVerticle

    public DiameterFlowExecutor(Vertx vertx, DiameterClientVerticle clientVerticle, AtomicInteger activeFlowsCounter) {
        this.vertx = vertx;
        this.clientVerticle = clientVerticle;
        this.activeFlowsCounter = activeFlowsCounter;
    }

    public Future<Void> executeFlow(DiameterFlow flow) {
        Promise<Void> flowPromise = Promise.promise();
        activeFlowsCounter.incrementAndGet();

        // Start the flow execution
        executeNextStep(flow, flowPromise);

        return flowPromise.future().eventually(v -> {
            activeFlowsCounter.decrementAndGet();
            System.out.println("Active flows after decrement: " + activeFlowsCounter.get() + " for flow " + flow.getKey());
            return Future.succeededFuture();
        });
    }

    private void executeNextStep(DiameterFlow currentFlow, Promise<Void> overallFlowPromise) {
        Buffer messageToSend = currentFlow.getNextMessage();

        if (messageToSend == null) {
            // Flow is complete by its own definition (no more messages)
            System.out.println("Flow " + currentFlow.getKey() + " completed (getNextMessage returned null before send).");
            overallFlowPromise.tryComplete(); // Use tryComplete in case it was failed by a previous error
            return;
        }

        clientVerticle.sendWithResponseHandler(messageToSend, responseBuffer -> {
            // 1. Parse responseBuffer to DiameterMessage
            DiameterMessage responseMessage = parseBufferToDiameterMessage(responseBuffer);
            if (responseMessage == null) {
                System.err.println("Failed to parse response for flow " + currentFlow.getKey());
                overallFlowPromise.tryFail("Response parsing failed for flow " + currentFlow.getKey());
                return;
            }

            if (responseMessage.getHeader() != null) {
                 System.out.println("Received response for flow " + currentFlow.getKey() +
                               ", Command: " + responseMessage.getHeader().getCommandCode() +
                               ", E2E: " + responseMessage.getHeader().getEndToEndId());
            } else {
                 System.out.println("Received response for flow " + currentFlow.getKey() + ", but header was null (parsing issue or invalid message)");
                 // Depending on strictness, might fail the flow here
            }


            // TODO: Add logic based on response if needed by the flow.
            // The current DiameterFlow interface doesn't have a method like "processResponseAndGetNextMessage".
            // For now, we assume the flow internally manages its state and getNextMessage() is sufficient.

            // Check if the flow wants to continue by calling getNextMessage() again.
            // This implies that getNextMessage() must be safe to call multiple times and
            // should advance the flow's internal state appropriately.
            executeNextStep(currentFlow, overallFlowPromise); // Recursive call to handle next step or complete

        }).onFailure(err -> {
            System.err.println("Failed to send message or handle response for flow " + currentFlow.getKey() + ": " + err.getMessage());
            overallFlowPromise.tryFail(err); // Use tryFail
        });
    }

    // Placeholder for parsing Buffer to DiameterMessage
    // In a real scenario, this would involve using the diameter library's parsing capabilities.
    private DiameterMessage parseBufferToDiameterMessage(Buffer buffer) {
        if (buffer == null || buffer.length() == 0) {
            System.err.println("Cannot parse null or empty buffer to DiameterMessage.");
            return null;
        }
        // This is a HACK. The DiameterMessage class expects a java.nio.ByteBuffer.
        // And it reads from the current position to the limit.
        // The constructor `new DiameterMessage(ByteBuffer)` is the one used in the old DiameterSocket.
        try {
            // The DiameterMessage constructor that takes ByteBuffer expects the buffer to be ready for reading (position at start, limit at end of data)
            // Vert.x buffer.getBytes() returns all bytes.
            ByteBuffer nioBuffer = ByteBuffer.wrap(buffer.getBytes());
            // No flip() needed here if DiameterMessage constructor reads from position 0 to limit.
            // If it expects position to be 0 and limit to be capacity after writing, then flip() would be needed before this.
            // Based on typical java.nio usage, wrap() sets position=0, limit=capacity.
            return new DiameterMessage(nioBuffer);
        } catch (Exception e) {
            // Catching generic Exception because the DiameterMessage constructor might throw various things
            // if the buffer isn't a valid Diameter message or due to other parsing issues.
            System.err.println("Error parsing Buffer to DiameterMessage: " + e.getMessage());
            // e.printStackTrace(); // For more details during development - can be noisy
            return null;
        }
    }
}
