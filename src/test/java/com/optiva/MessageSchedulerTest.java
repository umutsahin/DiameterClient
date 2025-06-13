package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.flows.DiameterFlow;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.Promise; // Added for MockDiameterClientVerticle
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.future.SucceededFuture;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

// Basic manual testing without JUnit or Mockito for simplicity in this environment
public class MessageSchedulerTest {

    // --- Manual Mocks & Stubs ---
    static class MockVertx extends io.vertx.core.impl.VertxImpl { // Extend VertxImpl for basic functionality if needed, or just interface
        public MockVertx() {
            super(null, new io.vertx.core.VertxOptions()); // Basic constructor
        }
        public long lastTimerInterval = -1;
        public Handler<Long> lastTimerHandler;
        public boolean timerCancelled = false;
        public int periodicTimerSetCount = 0;
        private long currentTimerId = 1L; // Dummy timer ID

        @Override
        public long setPeriodic(long delay, Handler<Long> handler) {
            this.lastTimerInterval = delay;
            this.lastTimerHandler = handler;
            this.periodicTimerSetCount++;
            this.timerCancelled = false; // Reset flag when new timer is set
            return currentTimerId++;
        }

        @Override
        public boolean cancelTimer(long id) {
            this.timerCancelled = true;
            return true;
        }
    }

    static class MockDiameterClientVerticle extends DiameterClientVerticle {
        public Buffer lastSentMessage;
        public String lastFlowKey;
        public Handler<DiameterMessage> lastResponseHandler;
        // public Handler<AsyncResult<Void>> lastWriteHandler; // Not used in current tests
        public boolean failNextSend = false;
        public DiameterMessage cannedResponse;

        // Constructor needed if superclass has specific constructor requirements
        public MockDiameterClientVerticle() {
            super(); // Assuming a no-arg constructor or handle appropriately
        }

        @Override
        public Future<Void> sendWithResponseHandler(Buffer message, String flowKey, Handler<DiameterMessage> responseHandler) {
            this.lastSentMessage = message;
            this.lastFlowKey = flowKey;
            this.lastResponseHandler = responseHandler;

            Promise<Void> promise = Promise.promise();
            if (failNextSend) {
                promise.fail("Simulated send failure");
            } else {
                promise.complete(); // Simulate successful write
                // if (this.lastResponseHandler != null && cannedResponse != null) {
                    // Test will call handler directly to simulate async response
                // }
            }
            return promise.future();
        }
    }

    static class MockDiameterFlow implements DiameterFlow {
        public String key = "testFlow";
        public Buffer messageToReturn = Buffer.buffer("TestMessage");
        public DiameterMessage lastProcessedResponse;
        public int getNextMessageCallCount = 0;
        public int processResponseCallCount = 0;
        public int terminateCallCount = 0;
        public boolean hasNextAfterResponse = false; // Controls if getNextMessage returns something after processResponse

        @Override
        public String getKey() { return key; }

        @Override
        public Buffer getNextMessage() {
            getNextMessageCallCount++;
            if (terminateCallCount > 0) return null; // If terminated, no more messages
            if (processResponseCallCount > 0) { // If processResponse has been called
                return hasNextAfterResponse ? Buffer.buffer("NextTestMessage") : null;
            }
            return messageToReturn; // Initial message
        }

        @Override
        public void processResponse(DiameterMessage responseMessage) {
            processResponseCallCount++;
            lastProcessedResponse = responseMessage;
        }

        @Override
        public DiameterFlow terminate() {
            terminateCallCount++;
            // messageToReturn = null; // getNextMessage handles this now
            // hasNextAfterResponse = false;
            return this;
        }

        @Override
        public boolean isInitialized() { return true; } // Keep simple for these tests

        @Override
        public DiameterFlow restart() { return this;} // Not used in these tests
    }

    // --- Test Methods ---
    public static void main(String[] args) {
        System.out.println("Running MessageSchedulerTest...");
        testScheduleFlow_AddsToQueueAndIncrementsActiveFlows();
        testProcessQueue_SendsMessageAndHandlesResponse_Requeues();
        testProcessQueue_SendsMessageAndHandlesResponse_Completes();
        testProcessQueue_HandlesSendFailure();
        testProcessQueue_HandlesInitialNullMessageFromFlow();
        testSetRps_StartsAndStopsTimer();
        System.out.println("MessageSchedulerTest completed successfully.");
    }

    private static void assertEqual(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) {
            // System.out.println("PASS: " + message);
        } else {
            System.err.println("AssertionError: FAIL: " + message + " - Expected: " + expected + ", Actual: " + actual);
            throw new AssertionError("FAIL: " + message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }
     private static void assertTrue(boolean condition, String message) {
        if (condition) {
            // System.out.println("PASS: " + message);
        } else {
            System.err.println("AssertionError: FAIL: " + message);
            throw new AssertionError("FAIL: " + message);
        }
    }


    private static void testScheduleFlow_AddsToQueueAndIncrementsActiveFlows() {
        System.out.print("testScheduleFlow_AddsToQueueAndIncrementsActiveFlows: ");
        MockVertx mockVertx = new MockVertx();
        MockDiameterClientVerticle mockClient = new MockDiameterClientVerticle();
        AtomicInteger activeFlows = new AtomicInteger(0);
        MessageScheduler scheduler = new MessageScheduler(mockVertx, mockClient, activeFlows);
        MockDiameterFlow mockFlow = new MockDiameterFlow();

        scheduler.scheduleFlow(mockFlow);

        assertEqual(1, activeFlows.get(), "Active flows should be 1 after scheduling");
        System.out.println("PASS");
    }

    private static void testProcessQueue_SendsMessageAndHandlesResponse_Requeues() {
        System.out.print("testProcessQueue_SendsMessageAndHandlesResponse_Requeues: ");
        MockVertx mockVertx = new MockVertx();
        MockDiameterClientVerticle mockClient = new MockDiameterClientVerticle();
        AtomicInteger activeFlows = new AtomicInteger(0);
        MessageScheduler scheduler = new MessageScheduler(mockVertx, mockClient, activeFlows);
        MockDiameterFlow mockFlow = new MockDiameterFlow();
        mockFlow.hasNextAfterResponse = true; // Configure mock flow to have a next message

        scheduler.scheduleFlow(mockFlow); // activeFlows = 1

        scheduler.processQueue(); // Manually trigger processQueue (as if timer fired)

        assertEqual(1, mockFlow.getNextMessageCallCount, "getNextMessage should be called once for initial send");
        assertTrue(mockClient.lastSentMessage != null, "A message should have been sent");
        assertEqual(mockFlow.key, mockClient.lastFlowKey, "Flow key should match");

        // Simulate response delivery
        DiameterMessage fakeResponse = new DiameterMessage(Buffer.buffer("FakeResponse").getByteBuf());
        mockClient.lastResponseHandler.handle(fakeResponse);

        assertEqual(1, mockFlow.processResponseCallCount, "processResponse should be called");
        assertEqual(fakeResponse, mockFlow.lastProcessedResponse, "Correct response should be processed");
        assertEqual(2, mockFlow.getNextMessageCallCount, "getNextMessage called again after response");
        assertEqual(1, activeFlows.get(), "Active flows should still be 1 as flow is re-queued");
        System.out.println("PASS");
    }

    private static void testProcessQueue_SendsMessageAndHandlesResponse_Completes() {
        System.out.print("testProcessQueue_SendsMessageAndHandlesResponse_Completes: ");
        MockVertx mockVertx = new MockVertx();
        MockDiameterClientVerticle mockClient = new MockDiameterClientVerticle();
        AtomicInteger activeFlows = new AtomicInteger(0);
        MessageScheduler scheduler = new MessageScheduler(mockVertx, mockClient, activeFlows);
        MockDiameterFlow mockFlow = new MockDiameterFlow();
        mockFlow.hasNextAfterResponse = false; // Configure mock flow to complete after this response

        scheduler.scheduleFlow(mockFlow); // activeFlows = 1
        scheduler.processQueue(); // Send first message

        DiameterMessage fakeResponse = new DiameterMessage(Buffer.buffer("FakeResponse").getByteBuf());
        mockClient.lastResponseHandler.handle(fakeResponse);

        assertEqual(1, mockFlow.processResponseCallCount, "processResponse should be called");
        assertEqual(2, mockFlow.getNextMessageCallCount, "getNextMessage called again after response"); // one for initial, one after response
        assertEqual(0, activeFlows.get(), "Active flows should be 0 as flow completes");
        System.out.println("PASS");
    }

    private static void testProcessQueue_HandlesSendFailure() {
        System.out.print("testProcessQueue_HandlesSendFailure: ");
        MockVertx mockVertx = new MockVertx();
        MockDiameterClientVerticle mockClient = new MockDiameterClientVerticle();
        AtomicInteger activeFlows = new AtomicInteger(0);
        MessageScheduler scheduler = new MessageScheduler(mockVertx, mockClient, activeFlows);
        MockDiameterFlow mockFlow = new MockDiameterFlow();

        mockClient.failNextSend = true; // Simulate send failure
        scheduler.scheduleFlow(mockFlow); // activeFlows = 1
        scheduler.processQueue(); // Attempt to send

        assertEqual(1, mockFlow.getNextMessageCallCount, "getNextMessage should be called for initial send attempt");
        assertEqual(1, mockFlow.terminateCallCount, "flow.terminate() should be called on send failure");
        assertEqual(0, activeFlows.get(), "Active flows should be 0 after send failure");
        System.out.println("PASS");
    }

    private static void testProcessQueue_HandlesInitialNullMessageFromFlow() {
        System.out.print("testProcessQueue_HandlesInitialNullMessageFromFlow: ");
        MockVertx mockVertx = new MockVertx();
        MockDiameterClientVerticle mockClient = new MockDiameterClientVerticle();
        AtomicInteger activeFlows = new AtomicInteger(0);
        MessageScheduler scheduler = new MessageScheduler(mockVertx, mockClient, activeFlows);
        MockDiameterFlow mockFlow = new MockDiameterFlow();

        mockFlow.messageToReturn = null; // Flow initially returns no message
        scheduler.scheduleFlow(mockFlow); // activeFlows = 1
        scheduler.processQueue(); // Attempt to process

        assertEqual(1, mockFlow.getNextMessageCallCount, "getNextMessage should be called once");
        assertTrue(mockClient.lastSentMessage == null, "No message should have been sent");
        assertEqual(0, activeFlows.get(), "Active flows should be 0 as flow immediately completes");
        assertEqual(0, mockFlow.terminateCallCount, "flow.terminate() should not be called for initial null message");
        System.out.println("PASS");
    }


    private static void testSetRps_StartsAndStopsTimer() {
        System.out.print("testSetRps_StartsAndStopsTimer: ");
        MockVertx mockVertx = new MockVertx();
        MockDiameterClientVerticle mockClient = new MockDiameterClientVerticle();
        AtomicInteger activeFlows = new AtomicInteger(0);
        MessageScheduler scheduler = new MessageScheduler(mockVertx, mockClient, activeFlows);

        scheduler.setRps(10); // Interval = 1000 / 10 = 100
        assertEqual(100L, mockVertx.lastTimerInterval, "Timer interval should be 100ms for 10 RPS");
        assertTrue(mockVertx.lastTimerHandler != null, "Timer handler should be set");
        assertEqual(1, mockVertx.periodicTimerSetCount, "Timer should be set once");
        assertTrue(!mockVertx.timerCancelled, "Timer should not be cancelled immediately after setting");

        scheduler.setRps(20); // Interval = 1000 / 20 = 50
        assertTrue(mockVertx.timerCancelled, "Previous timer should be cancelled");
        assertEqual(50L, mockVertx.lastTimerInterval, "Timer interval should be 50ms for 20 RPS");
        assertEqual(2, mockVertx.periodicTimerSetCount, "Timer should be set again");

        scheduler.setRps(0);
        assertTrue(mockVertx.timerCancelled, "Timer should be cancelled for 0 RPS");
        assertEqual(2, mockVertx.periodicTimerSetCount, "Timer set count should remain 2 as no new timer is set for 0 RPS");
        System.out.println("PASS");
    }
}
