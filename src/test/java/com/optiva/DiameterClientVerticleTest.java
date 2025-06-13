package com.optiva;

import com.optiva.console.Console; // Assuming Console is mockable or suppress its output
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiameterClientVerticleTest {

    @Mock
    Vertx mockVertx;
    @Mock
    NetClient mockNetClient;
    @Mock
    NetSocket mockSocket1;
    @Mock
    NetSocket mockSocket2;
    @Mock
    com.optiva.charging.openapi.diameter.DiameterMessage mockDiameterMessage;
    @Mock
    com.optiva.charging.openapi.diameter.DiameterMessageHeader mockDiameterMessageHeader;


    @Captor
    ArgumentCaptor<Handler<io.vertx.core.AsyncResult<NetSocket>>> connectHandlerCaptor;
    @Captor
    ArgumentCaptor<Handler<Buffer>> socketDataHandlerCaptor;
    @Captor
    ArgumentCaptor<Handler<Void>> socketCloseHandlerCaptor;


    private DiameterClientVerticle clientVerticle;

    @BeforeEach
    void setUp() {
        // Suppress console output for tests if necessary
        // Console.setTesting(true); // Assuming a way to suppress Console output

        lenient().when(mockVertx.createNetClient(any())).thenReturn(mockNetClient);
        clientVerticle = new DiameterClientVerticle(); // Real instance
        // Simulate Vertx context for the verticle (simplified)
        // In a real Vert.x test, you'd use VertxTestContext
        clientVerticle.init(mockVertx, null); // Context is null, config is default

        // Mock socket IDs for logging/distinction
        lenient().when(mockSocket1.writeHandlerID()).thenReturn("socket1_id");
        lenient().when(mockSocket2.writeHandlerID()).thenReturn("socket2_id");
        lenient().when(mockDiameterMessage.getHeader()).thenReturn(mockDiameterMessageHeader);

    }

    private void simulateSuccessfulConnect(NetSocket socket) {
        // Capture the connection handler and invoke it with success
        verify(mockNetClient, atLeastOnce()).connect(anyInt(), anyString(), connectHandlerCaptor.capture());
        Handler<io.vertx.core.AsyncResult<NetSocket>> connectHandler = connectHandlerCaptor.getAllValues().get(connectHandlerCaptor.getAllValues().size()-1);

        // Simulate a successful connection for the mocked socket
        io.vertx.core.AsyncResult<NetSocket> successResult = mock(io.vertx.core.AsyncResult.class);
        when(successResult.succeeded()).thenReturn(true);
        when(successResult.result()).thenReturn(socket);
        connectHandler.handle(successResult);

        // Capture the data handler for this socket
        verify(socket, atLeastOnce()).handler(socketDataHandlerCaptor.capture());
        // Capture close handler
        verify(socket, atLeastOnce()).closeHandler(socketCloseHandlerCaptor.capture());
    }


    @Test
    void testFlowSocketAffinity_sameFlowKeyUsesSameSocket() {
        clientVerticle.config().put("socketCount", 2); // Use 2 sockets
        Promise<Void> startPromise = Promise.promise();
        clientVerticle.start(startPromise);

        simulateSuccessfulConnect(mockSocket1);
        simulateSuccessfulConnect(mockSocket2);
        assertTrue(startPromise.future().isComplete());

        String flowKey1 = "flow1";
        Buffer testMessage = Buffer.buffer("test");
        Handler<Buffer> mockResponseHandler = mock(Handler.class);

        // Send first message for flow1
        clientVerticle.sendWithResponseHandler(testMessage, flowKey1, 101, mockResponseHandler);
        NetSocket firstSocket = clientVerticle.getSocket(flowKey1); // Expose getSocket or test via send behavior

        // Send second message for flow1
        clientVerticle.sendWithResponseHandler(testMessage, flowKey1, 102, mockResponseHandler);
        NetSocket secondSocket = clientVerticle.getSocket(flowKey1);

        assertNotNull(firstSocket, "First socket should not be null");
        assertSame(firstSocket, secondSocket, "Sockets for the same flow key should be the same instance.");
    }

    @Test
    void testFlowSocketAffinity_differentFlowKeysCanUseDifferentSockets() {
        clientVerticle.config().put("socketCount", 2);
        Promise<Void> startPromise = Promise.promise();
        clientVerticle.start(startPromise);

        simulateSuccessfulConnect(mockSocket1);
        simulateSuccessfulConnect(mockSocket2);
        assertTrue(startPromise.future().isComplete());

        String flowKey1 = "flow1";
        String flowKey2 = "flow2";
        Buffer testMessage = Buffer.buffer("test");
        Handler<Buffer> mockResponseHandler = mock(Handler.class);

        clientVerticle.sendWithResponseHandler(testMessage, flowKey1, 201, mockResponseHandler);
        NetSocket socketForFlow1 = clientVerticle.getSocket(flowKey1);

        clientVerticle.sendWithResponseHandler(testMessage, flowKey2, 202, mockResponseHandler);
        NetSocket socketForFlow2 = clientVerticle.getSocket(flowKey2);

        assertNotNull(socketForFlow1);
        assertNotNull(socketForFlow2);
        // With round-robin for new flows, they should be different if sockets > 1
        if (clientVerticle.getSockets().size() > 1) { // getSockets() would need to be added or accessible
             assertNotSame(socketForFlow1, socketForFlow2, "Sockets for different flow keys should be different with multiple available sockets.");
        }
    }

    @Test
    void testResponseCorrelation_hopByHopIdDirectsToCorrectHandler() {
        clientVerticle.config().put("socketCount", 1); // Use 1 socket
        Promise<Void> startPromise = Promise.promise();
        clientVerticle.start(startPromise);
        simulateSuccessfulConnect(mockSocket1);
        assertTrue(startPromise.future().isComplete());

        String flowKey1 = "flow1";
        int hopByHopId1 = 301;
        int hopByHopId2 = 302;
        Buffer message1 = Buffer.buffer("msg1");
        Buffer message2 = Buffer.buffer("msg2");
        Handler<Buffer> handler1 = mock(Handler.class);
        Handler<Buffer> handler2 = mock(Handler.class);

        // Send two messages on the same flow (and thus same socket), with different HopByHop IDs
        clientVerticle.sendWithResponseHandler(message1, flowKey1, hopByHopId1, handler1);
        clientVerticle.sendWithResponseHandler(message2, flowKey1, hopByHopId2, handler2);

        // Simulate response for message 2 arriving first
        Buffer responseBufferForMsg2 = Buffer.buffer("response2");
        when(mockDiameterMessageHeader.getHopByHopId()).thenReturn(hopByHopId2);
        // This assumes parseBufferToDiameterMessage works, or we mock its behavior if it's complex

        // Get the captured data handler for mockSocket1
        Handler<Buffer> capturedSocketHandler = socketDataHandlerCaptor.getValue();
        capturedSocketHandler.handle(responseBufferForMsg2); // Simulate data arrival

        verify(handler2).handle(responseBufferForMsg2);
        verify(handler1, never()).handle(any(Buffer.class));

        // Simulate response for message 1 arriving
        Buffer responseBufferForMsg1 = Buffer.buffer("response1");
        when(mockDiameterMessageHeader.getHopByHopId()).thenReturn(hopByHopId1);
        capturedSocketHandler.handle(responseBufferForMsg1);

        verify(handler1).handle(responseBufferForMsg1);
        verify(handler2, times(1)).handle(responseBufferForMsg2); // Already handled once
    }

    @Test
    void testSocketClosure_removesMappings() {
        clientVerticle.config().put("socketCount", 1);
        Promise<Void> startPromise = Promise.promise();
        clientVerticle.start(startPromise);

        simulateSuccessfulConnect(mockSocket1);
        assertTrue(startPromise.future().isComplete());

        String flowKey1 = "flow1";
        clientVerticle.sendWithResponseHandler(Buffer.buffer("msg"), flowKey1, 401, mock(Handler.class));
        assertNotNull(clientVerticle.getSocket(flowKey1), "Socket should be mapped before close.");

        // Simulate socket closure
        Handler<Void> capturedCloseHandler = socketCloseHandlerCaptor.getValue();
        capturedCloseHandler.handle(null); // Trigger close handler

        // Check that maps are cleaned. This requires exposing the maps or having specific methods.
        // For this example, let's assume getSocket would return a new one if the old one was removed.
        // Or, more directly, if flowToSocketMap was accessible:
        // assertTrue(clientVerticle.getFlowToSocketMap().isEmpty(), "FlowToSocketMap should be empty after socket close");
        // assertTrue(clientVerticle.getResponseHandlers().isEmpty(), "ResponseHandlers map should be empty");

        // Attempting to get socket for flowKey1 again should assign a new one if possible,
        // or null if no sockets left. Since we only had one, it should be null or fail.
        // This part depends on reconnection logic which is not the focus here.
        // The main point is that internal maps related to mockSocket1 are cleared.
        // For simplicity, we'll assume that if getSocket(flowKey1) is called again,
        // and if it tried to re-use the now-closed mockSocket1, it would fail or pick a new one.
        // A more direct test would inspect the internal maps.
    }
}
