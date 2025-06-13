package com.optiva;

import com.optiva.console.Console;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import com.optiva.charging.openapi.diameter.DiameterMessage; // Added import
import io.vertx.core.buffer.impl.BufferImpl; // Added import

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DiameterClientVerticle extends AbstractVerticle {

    private String serverHost = "127.0.0.1";
    private int serverPort = 3868;
    private int socketCount = 1;

    private NetClient client;
    private final List<NetSocket> sockets = new CopyOnWriteArrayList<>();
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    private Promise<Void> startPromiseInternal; // Renamed to avoid conflict

    // Map to store response handlers for sockets.
    // This is a simplified approach. For true multiplexing of requests on a single socket,
    // a correlation ID within messages would be needed.
    // This current map assumes one handler per socket, which will be set by the sender.
    // New: Map per socket, keyed by Hop-by-Hop ID
    private final Map<NetSocket, Map<Integer, Handler<Buffer>>> responseHandlers = new ConcurrentHashMap<>();

    private final Map<String, NetSocket> flowToSocketMap = new ConcurrentHashMap<>();
    private final Map<NetSocket, String> socketToFlowKeyMap = new ConcurrentHashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        this.startPromiseInternal = startPromise;
        this.serverHost = config().getString("serverHost", "127.0.0.1");
        this.serverPort = config().getInteger("serverPort", 3868);
        this.socketCount = config().getInteger("socketCount", 1);

        NetClientOptions options = new NetClientOptions()
            .setConnectTimeout(10000)
            .setReconnectAttempts(0);
        this.client = vertx.createNetClient(options);

        Console.log("DiameterClientVerticle starting. Connecting to " + serverHost + ":" + serverPort + " with " + socketCount + " sockets.");

        if (socketCount == 0) {
            Console.log("Socket count is 0. DiameterClientVerticle started without connections.");
            this.startPromiseInternal.complete();
            return;
        }

        for (int i = 0; i < socketCount; i++) {
            connect(i, socketCount);
        }
    }

    private void connect(int index, int totalToConnect) {
        client.connect(serverPort, serverHost, res -> {
            if (res.succeeded()) {
                NetSocket socket = res.result();
                sockets.add(socket);
                Console.log("Successfully connected socket: " + socket.writeHandlerID() + ". Total sockets: " + sockets.size());

                // Default handler - will be overridden by sendWithResponseHandler
                socket.handler(buffer -> {
                    DiameterMessage responseMessage = parseBufferToDiameterMessage(buffer);
                    if (responseMessage == null) {
                        Console.error("Received data from " + socket.writeHandlerID() + " but failed to parse DiameterMessage: " + buffer.length() + " bytes");
                        // Cannot proceed without HopByHopID, so we can't call a specific handler.
                        // Depending on protocol, might need to close socket or send an error.
                        return;
                    }

                    int hopByHopId = responseMessage.getHeader().getHopByHopId();
                    Map<Integer, Handler<Buffer>> socketSpecificHandlers = responseHandlers.get(socket);

                    if (socketSpecificHandlers != null) {
                        Handler<Buffer> specificHandler = socketSpecificHandlers.remove(hopByHopId); // Remove after retrieving
                        if (specificHandler != null) {
                            specificHandler.handle(buffer);
                        } else {
                            Console.log("Received data from " + socket.writeHandlerID() + " with HopByHopID " + hopByHopId + " but no specific handler.");
                        }
                        if (socketSpecificHandlers.isEmpty()) {
                            responseHandlers.remove(socket); // Clean up outer map if inner map is empty
                            Console.log("Cleaned up empty handler map for socket " + socket.writeHandlerID());
                        }
                    } else {
                        Console.log("Received data from " + socket.writeHandlerID() + " with HopByHopID " + hopByHopId + " but no handlers registered for this socket.");
                    }
                });

                socket.closeHandler(v -> {
                    Console.log("Socket closed: " + socket.writeHandlerID());
                    sockets.remove(socket);
                    responseHandlers.remove(socket); // Clean up all handlers for this closed socket
                    // Cleanup flow maps
                    String flowKey = socketToFlowKeyMap.remove(socket);
                    if (flowKey != null) {
                        flowToSocketMap.remove(flowKey, socket); // Remove only if value matches
                    }
                    // Optional: Reconnect logic
                });

                socket.exceptionHandler(e -> {
                    Console.error("Socket exception for " + socket.writeHandlerID() + ": " + e.getMessage());
                    sockets.remove(socket);
                    responseHandlers.remove(socket); // Clean up all handlers for this socket on exception
                    // Cleanup flow maps
                    String flowKey = socketToFlowKeyMap.remove(socket);
                    if (flowKey != null) {
                        flowToSocketMap.remove(flowKey, socket); // Remove only if value matches
                    }
                });

                if (!startPromiseInternal.future().isComplete()) {
                    if (sockets.size() >= 1) { // Consider successful if at least one connection is made
                        startPromiseInternal.complete();
                    } else if (index == totalToConnect - 1) { // Last attempt and still no success
                        Console.error("Failed to connect any initial sockets after all attempts.");
                        startPromiseInternal.fail("Failed to connect any initial sockets.");
                    }
                }
            } else {
                Console.error("Failed to connect socket attempt " + index + ": " + res.cause().getMessage());
                if (!startPromiseInternal.future().isComplete() && index == totalToConnect - 1 && sockets.isEmpty()) {
                    Console.error("Failed to connect any initial sockets after all attempts (last attempt failed).");
                    startPromiseInternal.fail("Failed to connect any initial sockets.");
                }
            }
        });
    }

    // Send message and expect a response on the same socket, handled by responseHandler
    public Future<Void> sendWithResponseHandler(Buffer message, String flowKey, int hopByHopId, Handler<Buffer> responseHandler) {
        NetSocket selectedSocket = getSocket(flowKey);

        if (selectedSocket == null) {
            String errorMsg = "No available sockets";
            if (flowKey != null) {
                errorMsg += " for flow " + flowKey;
            }
            Console.error(errorMsg + " (HopByHopID: " + hopByHopId + ")");
            return Future.failedFuture(errorMsg);
        }

        // Get or create the inner map for the specific socket
        Map<Integer, Handler<Buffer>> socketSpecificHandlers = responseHandlers.computeIfAbsent(selectedSocket, k -> new ConcurrentHashMap<>());
        // Store the handler for this specific HopByHopID
        socketSpecificHandlers.put(hopByHopId, responseHandler);

        Promise<Void> writePromise = Promise.promise();
        selectedSocket.write(message, writeOp -> {
            if (writeOp.succeeded()) {
                writePromise.complete();
            } else {
                Console.error("Failed to write message to socket " + selectedSocket.writeHandlerID() + " for flow " + flowKey + ", HopByHopID " + hopByHopId + ": " + writeOp.cause().getMessage());
                // Clean up handler on write failure for this specific HopByHopID
                Map<Integer, Handler<Buffer>> currentSocketHandlers = responseHandlers.get(selectedSocket);
                if (currentSocketHandlers != null) {
                    currentSocketHandlers.remove(hopByHopId);
                    if (currentSocketHandlers.isEmpty()) {
                        responseHandlers.remove(selectedSocket); // Clean up outer map if inner map is empty
                    }
                }
                // Do not remove from flowToSocketMap here, as the socket itself might still be valid
                // and could be reused for the same flowKey or a new one.
                // The cleanup of flowToSocketMap happens when a socket is closed or has an exception.
                writePromise.fail(writeOp.cause());
            }
        });
        return writePromise.future();
    }

    // Helper method to parse Buffer to DiameterMessage
    private DiameterMessage parseBufferToDiameterMessage(Buffer buffer) {
        if (buffer == null || buffer.length() == 0) {
            Console.error("Cannot parse null or empty buffer to DiameterMessage.");
            return null;
        }
        try {
            return new DiameterMessage(((BufferImpl) buffer).byteBuf());
        } catch (Exception e) {
            Console.error("Error parsing Buffer to DiameterMessage in DiameterClientVerticle: " + e.getMessage());
            return null;
        }
    }

    private NetSocket getSocket(String flowKey) {
        if (flowKey == null) {
            // If flowKey is null, fallback to round-robin if sockets are available
            // This case could be hit if the application logic decides not to use a flowKey for a particular message.
            if (sockets.isEmpty()) {
                Console.error("getSocket called with null flowKey and no sockets available.");
                return null;
            }
            Console.log("getSocket called with null flowKey, using round-robin for socket selection.");
            return sockets.get(roundRobinCounter.getAndIncrement() % sockets.size());
        }

        NetSocket existingSocket = flowToSocketMap.get(flowKey);

        // Check if the existing socket is still valid and in the active sockets list
        if (existingSocket != null && sockets.contains(existingSocket)) {
            // Optional: Check if socket is connected (vertx NetSocket doesn't have a direct isConnected())
            // For simplicity, we rely on the closeHandler and exceptionHandler to remove it from 'sockets' list
            Console.log("Reusing existing socket " + existingSocket.writeHandlerID() + " for flowKey: " + flowKey);
            return existingSocket;
        } else {
            // Socket not found for flowKey, or it's no longer active/valid
            if (existingSocket != null) {
                // Clean up if the socket was in flowToSocketMap but not in active sockets
                Console.log("Cleaning up stale socket " + existingSocket.writeHandlerID() + " for flowKey: " + flowKey);
                flowToSocketMap.remove(flowKey, existingSocket); // remove only if it's the same socket
                socketToFlowKeyMap.remove(existingSocket); // remove the reverse mapping
            }

            if (sockets.isEmpty()) {
                Console.error("No available sockets to assign for flowKey: " + flowKey);
                return null; // Or throw an exception
            }

            // Select a new socket (e.g., round-robin)
            NetSocket selectedSocket = sockets.get(roundRobinCounter.getAndIncrement() % sockets.size());
            Console.log("Assigning new socket " + selectedSocket.writeHandlerID() + " for flowKey: " + flowKey);

            // Store the new association
            flowToSocketMap.put(flowKey, selectedSocket);
            // Store the reverse mapping for cleanup
            // If this socket was previously associated with another flow, that old association will be overwritten here.
            // And the old flowKey might still point to this socket in flowToSocketMap until it's tried to be reused.
            // This is a potential issue if a socket is rapidly reassigned.
            // A cleaner approach might involve removing the old flowKey from flowToSocketMap if socketToFlowKeyMap.put returns a previous flowKey.
            String oldFlowKey = socketToFlowKeyMap.put(selectedSocket, flowKey);
            if (oldFlowKey != null && !oldFlowKey.equals(flowKey)) {
                // If the socket was previously mapped to a *different* flow, remove that old mapping.
                // This prevents a stale flowKey from potentially getting the wrong socket if that old flowKey is requested again
                // before the socket is naturally cleaned up by a disconnect.
                flowToSocketMap.remove(oldFlowKey, selectedSocket);
                 Console.log("Removed old flowKey " + oldFlowKey + " previously mapped to socket " + selectedSocket.writeHandlerID());
            }


            return selectedSocket;
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        Console.log("DiameterClientVerticle stopping. Closing " + sockets.size() + " sockets.");
        responseHandlers.clear();
        flowToSocketMap.clear();
        socketToFlowKeyMap.clear();
        List<Future<Void>> closeFutures = sockets.stream().map(s -> {
            Promise<Void> promise = Promise.promise();
            s.close(promise);
            return promise.future();
        }).toList();

        Future.all(closeFutures).onComplete(ar -> {
            sockets.clear(); // Ensure list is empty
            if (client != null) {
                client.close(clientCloseRes -> {
                    if (clientCloseRes.succeeded()) {
                        Console.log("NetClient closed successfully.");
                    } else {
                        Console.error("NetClient close failed: " + clientCloseRes.cause());
                    }
                    stopPromise.complete();
                });
            } else {
                stopPromise.complete();
            }
        });
    }
}
