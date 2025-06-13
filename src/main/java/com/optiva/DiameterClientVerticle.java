package com.optiva;

import com.optiva.OpenTelemetryConfig; // Add this
import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.console.Console;
import com.optiva.flows.DiameterFlow;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DiameterClientVerticle extends AbstractVerticle {

    private String serverHost = "127.0.0.1";
    private int serverPort = 3868;
    private int socketCount = 1;

    private NetClient client;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    private Promise<Void> startPromiseInternal;
    private final List<NetSocket> sockets = new CopyOnWriteArrayList<>();
    private final Map<String, NetSocket> flowToSocketMap = new ConcurrentHashMap<>();
    private final Map<NetSocket, Map<String, Handler<DiameterMessage>>> responseHandlers = new ConcurrentHashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        this.startPromiseInternal = startPromise;
        this.serverHost = config().getString("serverHost", "127.0.0.1");
        this.serverPort = config().getInteger("serverPort", 3868);
        this.socketCount = config().getInteger("socketCount", 1);

        NetClientOptions options = new NetClientOptions().setConnectTimeout(10000).setReconnectAttempts(0);
        this.client = vertx.createNetClient(options);

        Console.debug("DiameterClientVerticle starting. Connecting to "
                      + serverHost
                      + ":"
                      + serverPort
                      + " with "
                      + socketCount
                      + " sockets.");

        if (socketCount == 0) {
            Console.debug("Socket count is 0. DiameterClientVerticle started without connections.");
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
                Console.debug("Successfully connected socket: "
                              + socket.writeHandlerID()
                              + ". Total sockets: "
                              + sockets.size());

                // Default handler - will be overridden by sendWithResponseHandler
                socket.handler(buffer -> {
                    DiameterMessage responseMessage = parseBufferToDiameterMessage(buffer);
                    if (responseMessage == null) {
                        Console.error("Received data from "
                                      + socketAddress(socket)
                                      + " but failed to parse DiameterMessage: "
                                      + buffer.length()
                                      + " bytes");
                        return;
                    }
                    Map<String, Handler<DiameterMessage>> socketSpecificHandlers = responseHandlers.get(socket);
                    String flowKey = DiameterFlow.getKey(responseMessage);
                    if (socketSpecificHandlers != null) {
                        Handler<DiameterMessage> specificHandler
                                = socketSpecificHandlers.remove(flowKey); // Remove after retrieving
                        if (specificHandler != null) {
                            specificHandler.handle(responseMessage);
                        } else {
                            Console.debug("Received data from "
                                          + socket.writeHandlerID()
                                          + " with FlowKey "
                                          + flowKey
                                          + " but no specific handler.");
                        }
                        if (socketSpecificHandlers.isEmpty()) {
                            responseHandlers.remove(socket); // Clean up outer map if inner map is empty
                            Console.debug("Cleaned up empty handler map for socket " + socketAddress(socket));
                        }
                    } else {
                        Console.debug("Received data from "
                                      + socket.writeHandlerID()
                                      + " with FlowKey "
                                      + flowKey
                                      + " but no handlers registered for this socket.");
                    }
                });

                socket.closeHandler(v -> {
                    Console.debug("Socket closed: " + socketAddress(socket));
                    cleanup(socket);
                });

                socket.exceptionHandler(e -> {
                    Console.error("Socket exception for " + socketAddress(socket) + ": " + e.getMessage());
                    cleanup(socket);
                });

                if (!startPromiseInternal.future().isComplete()) {
                    if (!sockets.isEmpty()) {
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

    private void cleanup(NetSocket socket) {
        sockets.remove(socket);
        responseHandlers.remove(socket);
        Set<String> flowsToBeCleaned = flowToSocketMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(socket))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        flowToSocketMap.keySet().removeAll(flowsToBeCleaned);
    }

    // Send message and expect a response on the same socket, handled by responseHandler
    public Future<Void> sendWithResponseHandler(Buffer message,
                                                String flowKey,
                                                Handler<DiameterMessage> responseHandler) {
        NetSocket selectedSocket = getSocket(flowKey);

        if (selectedSocket == null) {
            String errorMsg = "No available sockets";
            if (flowKey != null) {
                errorMsg += " for flow " + flowKey;
            }
            Console.error(errorMsg + " (FlowKey: " + flowKey + ")");
            return Future.failedFuture(errorMsg);
        }

        // Get or create the inner map for the specific socket
        Map<String, Handler<DiameterMessage>> socketSpecificHandlers = responseHandlers.computeIfAbsent(selectedSocket,
                                                                                                        k -> new ConcurrentHashMap<>());
        // Store the handler for this specific HopByHopID
        socketSpecificHandlers.put(flowKey, responseHandler);

        Promise<Void> writePromise = Promise.promise();
        selectedSocket.write(message, writeOp -> {
            if (writeOp.succeeded()) {
                writePromise.complete();
                // Increment messages.sent counter
                OpenTelemetryConfig.getMessagesSentCounter().add(1); // Add this line
            } else {
                Console.error("Failed to write message to socket "
                              + socketAddress(selectedSocket)
                              + " for flow "
                              + flowKey
                              + ": "
                              + writeOp.cause().getMessage());
                // Clean up handler on write failure for this specific HopByHopID
                Map<String, Handler<DiameterMessage>> currentSocketHandlers = responseHandlers.get(selectedSocket);
                if (currentSocketHandlers != null) {
                    currentSocketHandlers.remove(flowKey);
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
            if (sockets.isEmpty()) {
                Console.error("getSocket called with null flowKey and no sockets available.");
                return null;
            }
            Console.debug("getSocket called with null flowKey, using round-robin for socket selection.");
            return sockets.get(roundRobinCounter.getAndIncrement() % sockets.size());
        }

        NetSocket existingSocket = flowToSocketMap.get(flowKey);

        if (existingSocket != null && sockets.contains(existingSocket)) {
            Console.debug("Reusing existing socket " + socketAddress(existingSocket) + " for flowKey: " + flowKey);
            return existingSocket;
        } else {
            if (existingSocket != null) {
                Console.debug("Cleaning up stale socket " + existingSocket.writeHandlerID() + " for flowKey: " + flowKey);
                flowToSocketMap.remove(flowKey, existingSocket);
            }

            if (sockets.isEmpty()) {
                Console.error("No available sockets to assign for flowKey: " + flowKey);
                return null; // Or throw an exception
            }

            NetSocket selectedSocket = sockets.get(roundRobinCounter.getAndIncrement() % sockets.size());
            Console.debug("Assigning new socket " + socketAddress(selectedSocket) + " for flowKey: " + flowKey);
            flowToSocketMap.put(flowKey, selectedSocket);
            return selectedSocket;
        }
    }

    private static String socketAddress(NetSocket existingSocket) {
        return existingSocket.localAddress() + "->" + existingSocket.remoteAddress();
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        Console.debug("DiameterClientVerticle stopping. Closing " + sockets.size() + " sockets.");
        responseHandlers.clear();
        flowToSocketMap.clear();
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
                        Console.debug("NetClient closed successfully.");
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
