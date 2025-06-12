package com.optiva;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

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
    private final Map<NetSocket, Handler<Buffer>> responseHandlers = new ConcurrentHashMap<>();

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

        System.out.println("DiameterClientVerticle starting. Connecting to " + serverHost + ":" + serverPort + " with " + socketCount + " sockets.");

        if (socketCount == 0) {
            System.out.println("Socket count is 0. DiameterClientVerticle started without connections.");
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
                System.out.println("Successfully connected socket: " + socket.writeHandlerID() + ". Total sockets: " + sockets.size());

                // Default handler - will be overridden by sendWithResponseHandler
                socket.handler(buffer -> {
                    Handler<Buffer> specificHandler = responseHandlers.get(socket);
                    if (specificHandler != null) {
                        specificHandler.handle(buffer);
                    } else {
                        System.out.println("Received data from " + socket.writeHandlerID() + " but no specific handler: " + buffer.length() + " bytes");
                    }
                });

                socket.closeHandler(v -> {
                    System.out.println("Socket closed: " + socket.writeHandlerID());
                    sockets.remove(socket);
                    responseHandlers.remove(socket);
                    // Optional: Reconnect logic
                });

                socket.exceptionHandler(e -> {
                    System.err.println("Socket exception for " + socket.writeHandlerID() + ": " + e.getMessage());
                    sockets.remove(socket);
                    responseHandlers.remove(socket);
                });

                if (!startPromiseInternal.future().isComplete()) {
                    if (sockets.size() >= 1) { // Consider successful if at least one connection is made
                        startPromiseInternal.complete();
                    } else if (index == totalToConnect - 1) { // Last attempt and still no success
                        System.err.println("Failed to connect any initial sockets after all attempts.");
                        startPromiseInternal.fail("Failed to connect any initial sockets.");
                    }
                }
            } else {
                System.err.println("Failed to connect socket attempt " + index + ": " + res.cause().getMessage());
                if (!startPromiseInternal.future().isComplete() && index == totalToConnect - 1 && sockets.isEmpty()) {
                    System.err.println("Failed to connect any initial sockets after all attempts (last attempt failed).");
                    startPromiseInternal.fail("Failed to connect any initial sockets.");
                }
            }
        });
    }

    // Send message and expect a response on the same socket, handled by responseHandler
    public Future<Void> sendWithResponseHandler(Buffer message, Handler<Buffer> responseHandler) {
        if (sockets.isEmpty()) {
            return Future.failedFuture("No available sockets to send message.");
        }
        // Simple round-robin for now
        NetSocket selectedSocket = sockets.get(roundRobinCounter.getAndIncrement() % sockets.size());

        // Store the handler for this socket. This overwrites any previous handler for this socket.
        responseHandlers.put(selectedSocket, responseHandler);

        Promise<Void> writePromise = Promise.promise();
        selectedSocket.write(message, writeOp -> {
            if (writeOp.succeeded()) {
                writePromise.complete();
            } else {
                System.err.println("Failed to write message to socket " + selectedSocket.writeHandlerID() + ": " + writeOp.cause().getMessage());
                responseHandlers.remove(selectedSocket); // Clean up handler on write failure
                writePromise.fail(writeOp.cause());
            }
        });
        return writePromise.future();
    }


    @Override
    public void stop(Promise<Void> stopPromise) {
        System.out.println("DiameterClientVerticle stopping. Closing " + sockets.size() + " sockets.");
        responseHandlers.clear();
        List<Future> closeFutures = new ArrayList<>();
        for (NetSocket socket : new ArrayList<>(sockets)) { // Iterate over a copy for safe removal
            Promise<Void> p = Promise.promise();
            socket.close(p); // close() is idempotent
            closeFutures.add(p.future());
        }

        Future.all(closeFutures).onComplete(ar -> {
            sockets.clear(); // Ensure list is empty
            if (client != null) {
                client.close(clientCloseRes -> {
                    if (clientCloseRes.succeeded()) {
                        System.out.println("NetClient closed successfully.");
                    } else {
                        System.err.println("NetClient close failed: " + clientCloseRes.cause());
                    }
                    stopPromise.complete();
                });
            } else {
                stopPromise.complete();
            }
        });
    }
}
