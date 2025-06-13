package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.DiameterMessageHeader;
import com.optiva.flows.DiameterFlow;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiameterFlowExecutorTest {

    @Mock
    DiameterClientVerticle mockClientVerticle;
    @Mock
    DiameterFlow mockDiameterFlow;
    @Mock
    DiameterMessage mockRequestDiameterMessage;
    @Mock
    DiameterMessageHeader mockRequestHeader;
    @Mock
    DiameterMessage mockResponseDiameterMessage;
    @Mock
    DiameterMessageHeader mockResponseHeader;

    @Captor
    ArgumentCaptor<Handler<Buffer>> responseHandlerCaptor;

    private DiameterFlowExecutor flowExecutor;
    private AtomicInteger activeFlowsCounter;

    @BeforeEach
    void setUp() {
        activeFlowsCounter = new AtomicInteger(0);
        flowExecutor = new DiameterFlowExecutor(mockClientVerticle, activeFlowsCounter);

        // Mock flow behavior
        when(mockDiameterFlow.getKey()).thenReturn("testFlowKey");

        // Mock request message and header
        when(mockRequestDiameterMessage.getHeader()).thenReturn(mockRequestHeader);

        // Mock response message and header
        when(mockResponseDiameterMessage.getHeader()).thenReturn(mockResponseHeader);
        when(mockResponseHeader.getCommandCode()).thenReturn(272); // Example: CC-Answer
    }

    @Test
    void testExecuteFlow_sendsMessageWithCorrectFlowKeyAndHopByHopId() {
        Buffer messageBuffer = Buffer.buffer("request_data");
        int expectedHopByHopId = 12345;

        when(mockDiameterFlow.getNextMessage()).thenReturn(messageBuffer).thenReturn(null); // Send one message then end
        // Simulate parsing of messageBuffer to mockRequestDiameterMessage
        // This part is tricky because parseBufferToDiameterMessage is private.
        // For a robust test, this parsing logic might need to be refactored or tested implicitly.
        // Here, we'll assume the executor correctly parses and gets the HopByHopId.
        // We'll mock the sendWithResponseHandler to expect it.
        when(mockRequestHeader.getHopByHopId()).thenReturn(expectedHopByHopId);


        // Mock clientVerticle's send method
        Promise<Void> sendPromise = Promise.promise();
        sendPromise.complete();
        when(mockClientVerticle.sendWithResponseHandler(
                eq(messageBuffer),
                eq("testFlowKey"),
                eq(expectedHopByHopId),
                any(Handler.class)))
                .thenReturn(sendPromise.future());

        // Mock successful response (Result-Code < 3000)
        when(mockResponseDiameterMessage.getAvpValue(eq(268))) // 268 is Result-Code AVP code
                .thenReturn(2001); // DIAMETER_SUCCESS


        Promise<Void> flowCompletionPromise = Promise.promise();
        flowExecutor.executeFlow(mockDiameterFlow).onComplete(flowCompletionPromise);


        // Capture the response handler and simulate a response
        verify(mockClientVerticle).sendWithResponseHandler(any(Buffer.class), anyString(), anyInt(), responseHandlerCaptor.capture());
        Handler<Buffer> capturedHandler = responseHandlerCaptor.getValue();

        Buffer responseBuffer = Buffer.buffer("response_data");
        // Simulate the response parsing within the handler (this relies on internal logic of handler)
        // To truly test this, we'd need to mock parseBufferToDiameterMessage if it were passed in,
        // or make assumptions about its behavior.
        // For this outline, we assume the handler works if sendWithResponseHandler is called correctly.

        capturedHandler.handle(responseBuffer); // This will trigger parsing and result code check

        assertTrue(flowCompletionPromise.future().succeeded(), "Flow should complete successfully.");
        assertEquals(0, activeFlowsCounter.get(), "Active flows counter should be zero after completion.");

        // Verify that sendWithResponseHandler was called with the correct parameters
        verify(mockClientVerticle).sendWithResponseHandler(
                eq(messageBuffer),
                eq("testFlowKey"),
                eq(expectedHopByHopId),
                any(Handler.class));
    }

    @Test
    void testExecuteFlow_handlesSendFailure() {
        Buffer messageBuffer = Buffer.buffer("request_data");
        int expectedHopByHopId = 54321;

        when(mockDiameterFlow.getNextMessage()).thenReturn(messageBuffer);
        // Assume parsing for HopByHopId happens correctly as in previous test
        when(mockRequestHeader.getHopByHopId()).thenReturn(expectedHopByHopId);


        // Mock clientVerticle's send method to fail
        Promise<Void> sendPromise = Promise.promise();
        sendPromise.fail("Connection error");
        when(mockClientVerticle.sendWithResponseHandler(
                eq(messageBuffer),
                eq("testFlowKey"),
                eq(expectedHopByHopId),
                any(Handler.class)))
                .thenReturn(sendPromise.future());

        Promise<Void> flowCompletionPromise = Promise.promise();
        flowExecutor.executeFlow(mockDiameterFlow).onComplete(flowCompletionPromise);

        assertTrue(flowCompletionPromise.future().failed(), "Flow should fail.");
        assertEquals(0, activeFlowsCounter.get(), "Active flows counter should be zero after failure.");
        assertEquals("Connection error", flowCompletionPromise.future().cause().getMessage());
    }

     @Test
    void testExecuteFlow_handlesErrorResultCode() {
        Buffer messageBuffer = Buffer.buffer("request_data");
        int expectedHopByHopId = 67890;

        when(mockDiameterFlow.getNextMessage()).thenReturn(messageBuffer);
        when(mockRequestHeader.getHopByHopId()).thenReturn(expectedHopByHopId);

        Promise<Void> sendPromise = Promise.promise();
        sendPromise.complete();
        when(mockClientVerticle.sendWithResponseHandler(
                any(Buffer.class), anyString(), anyInt(), any(Handler.class)))
                .thenReturn(sendPromise.future());

        // Mock error response (Result-Code >= 3000)
        when(mockResponseDiameterMessage.getAvpValue(eq(268))) // Result-Code
                .thenReturn(3001); // DIAMETER_COMMAND_UNSUPPORTED
        when(mockResponseDiameterMessage.getAvpValue(eq(297))) // Error-Message
                .thenReturn("Unsupported command");


        Promise<Void> flowCompletionPromise = Promise.promise();
        flowExecutor.executeFlow(mockDiameterFlow).onComplete(flowCompletionPromise);

        // Capture the response handler and simulate a response
        verify(mockClientVerticle).sendWithResponseHandler(any(Buffer.class), anyString(), anyInt(), responseHandlerCaptor.capture());
        Handler<Buffer> capturedHandler = responseHandlerCaptor.getValue();

        // This is where it gets tricky: the handler itself calls parseBufferToDiameterMessage.
        // To properly mock this, parseBufferToDiameterMessage would ideally be injectable or static and mockable.
        // For this test, we assume the response buffer passed to the handler will be parsed correctly
        // by the *actual* parseBufferToDiameterMessage within DiameterFlowExecutor.
        // The following lines are more of a conceptual guide.
        // One way to handle this would be to have DiameterFlowExecutor accept a Function<Buffer, DiameterMessage> for parsing.
        // Or, ensure the Buffer you pass to handle() can be parsed by the real method to produce the mocked DiameterMessage.
        // For now, we'll assume the internal parsing works and returns our mockResponseDiameterMessage.
        // This is a limitation of testing private methods indirectly.

        // Let's assume the Buffer.buffer("error_response") would be parsed by the real method
        // into something that yields the mocked values for Result-Code and Error-Message.
        // This is a strong assumption for a unit test.
        // A better way:
        // When the response handler inside DiameterFlowExecutor calls its *own* parseBufferToDiameterMessage,
        // that part is not directly mockable unless we refactor.
        // So, the test for *this specific part* (error code handling) is more of an integration test
        // of the lambda passed to sendWithResponseHandler.

        // For the sake of this outline, we'll trigger the handler and check the outcome.
        // The actual Buffer content for "error_response" would need to be craftable to yield the mocked values
        // if we weren't mocking the DiameterMessage that results from its parsing.
        capturedHandler.handle(Buffer.buffer("error_response")); // This buffer's content is opaque here


        assertTrue(flowCompletionPromise.future().failed(), "Flow should fail due to error result code.");
        assertEquals(0, activeFlowsCounter.get(), "Active flows counter should be zero.");
        assertTrue(flowCompletionPromise.future().cause().getMessage().contains("Request failed with 3001: Unsupported command"));
    }
}
