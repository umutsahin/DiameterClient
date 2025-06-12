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

        clientVerticle.sendWithResponseHandler(messageToSend, responseBuffer -> {
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
                               + ", E2E: "
                               + responseMessage.getHeader().getEndToEndId());

            Integer resultCode = responseMessage.<Integer>getAvpValue(RESULT_CODE);
            if (resultCode < 3000) {
                executeNextStep(currentFlow, overallFlowPromise); // Recursive call to handle next step or complete
            } else {
                String errorMsg = responseMessage.getAvpValue(ERROR_MESSAGE);
                overallFlowPromise.tryFail("Request failed with " + resultCode + ": " + errorMsg);
            }

        }).onFailure(err -> {
            Console.error("Failed to send message or handle response for flow "
                               + currentFlow.getKey()
                               + ": "
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
