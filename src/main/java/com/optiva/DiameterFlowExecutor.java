package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.console.Console;
import com.optiva.flows.DiameterFlow;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;

import java.util.concurrent.atomic.AtomicInteger;

import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ERROR_MESSAGE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.RESULT_CODE;

public class DiameterFlowExecutor {
    private final DiameterClientVerticle clientVerticle;
    private final AtomicInteger activeFlowsCounter;

    public DiameterFlowExecutor(DiameterClientVerticle clientVerticle, AtomicInteger activeFlowsCounter) {
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
            Console.log("Active flows after decrement: "
                        + activeFlowsCounter.get()
                        + " for flow "
                        + flow.getKey());
            return Future.succeededFuture();
        });
    }

    private void executeNextStep(DiameterFlow currentFlow, Promise<Void> overallFlowPromise) {
        Buffer messageToSend = currentFlow.getNextMessage();

        if (messageToSend == null) {
            // Flow is complete by its own definition (no more messages)
            Console.log("Flow "
                               + currentFlow.getKey()
                               + " completed (getNextMessage returned null before send).");
            overallFlowPromise.tryComplete(); // Use tryComplete in case it was failed by a previous error
            return;
        }

        // Parse the outgoing message to get HopByHopID
        DiameterMessage outgoingMessage = parseBufferToDiameterMessage(messageToSend);
        if (outgoingMessage == null) {
            Console.error("Failed to parse outgoing message for flow " + currentFlow.getKey() + ". Cannot extract HopByHopID.");
            overallFlowPromise.tryFail("Outgoing message parsing failed for flow " + currentFlow.getKey());
            return;
        }
        int hopByHopId = outgoingMessage.getHeader().getHopByHopId();

        clientVerticle.sendWithResponseHandler(messageToSend, currentFlow.getKey(), hopByHopId, responseBuffer -> {
            // 1. Parse responseBuffer to DiameterMessage
            DiameterMessage responseMessage = parseBufferToDiameterMessage(responseBuffer);
            if (responseMessage == null) {
                Console.error("Failed to parse response for flow " + currentFlow.getKey());
                overallFlowPromise.tryFail("Response parsing failed for flow " + currentFlow.getKey());
                return;
            }

            Console.log("Received response for flow "
                               + currentFlow.getKey()
                               + ", Command: "
                               + responseMessage.getHeader().getCommandCode()
                               + ", HopByHop: " + responseMessage.getHeader().getHopByHopId() // Log HopByHop for verification
                               + ", E2E: "
                               + responseMessage.getHeader().getEndToEndId());

            Integer resultCode = responseMessage.<Integer>getAvpValue(RESULT_CODE);
            if (resultCode < 3000) {
                // Before executing next step, ensure the HopByHopID of response matches request
                // This is an additional check, main correlation is done by DiameterClientVerticle
                if (responseMessage.getHeader().getHopByHopId() != hopByHopId) {
                    Console.error("HopByHopID mismatch for flow " + currentFlow.getKey() +
                                  ". Expected: " + hopByHopId +
                                  ", Received: " + responseMessage.getHeader().getHopByHopId());
                    // overallFlowPromise.tryFail("HopByHopID mismatch for flow " + currentFlow.getKey());
                    // Decide if this should be a fatal flow error or just a warning.
                    // For now, logging and continuing, as the client verticle should have routed it correctly.
                }
                executeNextStep(currentFlow, overallFlowPromise); // Recursive call to handle next step or complete
            } else {
                String errorMsg = responseMessage.getAvpValue(ERROR_MESSAGE);
                overallFlowPromise.tryFail("Request failed with " + resultCode + ": " + errorMsg);
            }

        }).onFailure(err -> {
            Console.error("Failed to send message or handle response for flow "
                               + currentFlow.getKey()
                               + " (HopByHopID: " + hopByHopId + "): "
                               + err.getMessage());
            overallFlowPromise.tryFail(err); // Use tryFail
        });
    }

    private DiameterMessage parseBufferToDiameterMessage(Buffer buffer) {
        if (buffer == null || buffer.length() == 0) {
            Console.error("Cannot parse null or empty buffer to DiameterMessage.");
            return null;
        }
        try {
            return new DiameterMessage(((BufferImpl) buffer).byteBuf());
        } catch (Exception e) {
            Console.error("Error parsing Buffer to DiameterMessage: " + e.getMessage());
            return null;
        }
    }
}
