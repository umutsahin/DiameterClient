package com.optiva;

import com.optiva.console.Console;
import com.optiva.flows.DiameterFlow;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;

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
            Console.debug("Active flows after decrement: " + activeFlowsCounter.get() + " for flow " + flow.getKey());
            return Future.succeededFuture();
        });
    }

    private void executeNextStep(DiameterFlow currentFlow, Promise<Void> overallFlowPromise) {
        Buffer messageToSend = currentFlow.getNextMessage();

        if (messageToSend == null) {
            // Flow is complete by its own definition (no more messages)
            Console.debug("Flow " + currentFlow.getKey() + " completed.");
            overallFlowPromise.tryComplete(); // Use tryComplete in case it was failed by a previous error
            return;
        }

        clientVerticle.sendWithResponseHandler(messageToSend, currentFlow.getKey(), responseMessage -> {
            Console.debug("Received response for flow "
                          + currentFlow.getKey()
                          + ", Command: "
                          + responseMessage.getHeader().getCommandCode()
                          + ", HopByHop: "
                          + responseMessage.getHeader().getHopByHopId()
                          // Log HopByHop for verification
                          + ", E2E: "
                          + responseMessage.getHeader().getEndToEndId());

            Integer resultCode = responseMessage.<Integer>getAvpValue(RESULT_CODE);
            if (resultCode < 3000) {
                String flowKey = DiameterFlow.getKey(responseMessage);
                if (!currentFlow.getKey().equals(flowKey)) {
                    Console.error("Flow mismatch." + "Expected: " + currentFlow.getKey() + ", Received: " + flowKey);
                }
                executeNextStep(currentFlow, overallFlowPromise); // Recursive call to handle next step or complete
            } else {
                String errorMsg = responseMessage.getAvpValue(ERROR_MESSAGE);
                if (currentFlow.isInitialized()) {
                    Console.error("Request failed with " + resultCode + ": " + errorMsg + ", terminating flow...");
                    executeNextStep(currentFlow.terminate(), overallFlowPromise);
                } else {
                    overallFlowPromise.tryFail("Request failed with " + resultCode + ": " + errorMsg);
                }
            }

        }).onFailure(err -> {
            Console.error("Failed to send message or handle response for flow "
                          + currentFlow.getKey()
                          + ": "
                          + err.getMessage());
            overallFlowPromise.tryFail(err); // Use tryFail
        });
    }
}
