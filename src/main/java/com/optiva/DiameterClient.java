package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.console.Console;
import com.optiva.flows.DiameterFlow;
import io.netty.buffer.ByteBuf;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.optiva.observability.OpenTelemetryConfig.messagesSentCounter;

public class DiameterClient {

    private final String serverHost;
    private final int serverPort;
    private final NetClient client;
    private NetSocket socket;
    private final Map<String, Handler<DiameterMessage>> responseHandlers = new ConcurrentHashMap<>();

    public DiameterClient(Vertx vertx, String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        NetClientOptions options = new NetClientOptions().setConnectTimeout(10000)
                .setReconnectAttempts(0)
                .setReceiveBufferSize(8196)
                .setSendBufferSize(8196);
        this.client = vertx.createNetClient(options);
    }

    public Future<Void> connect() {
        Promise<Void> connectPromise = Promise.promise();
        client.connect(serverPort, serverHost, res -> {
            if (res.succeeded()) {
                NetSocket newSocket = res.result();
                this.socket = newSocket;
                Console.trace("Successfully connected socket: " + socketAddress(newSocket) + ".");
                newSocket.handler(buffer -> {
                    ByteBuf byteBuf = ((BufferImpl) buffer).byteBuf();
                    int wi = byteBuf.writerIndex();
                    do {
                        byteBuf.writerIndex(wi);
                        DiameterMessage responseMessage = new DiameterMessage(byteBuf);
                        String flowKey = DiameterFlow.getKey(responseMessage);
                        Handler<DiameterMessage> handler = responseHandlers.remove(flowKey);
                        if (handler != null) {
                            handler.handle(responseMessage);
                        } else {
                            Console.trace("Received data from "
                                          + socketAddress(newSocket)
                                          + " with FlowKey "
                                          + flowKey
                                          + " but no specific handler.");
                        }
                    } while (wi != byteBuf.writerIndex());
                });

                newSocket.closeHandler(v -> {
                    Console.trace("Socket closed: " + socketAddress(newSocket));
                    this.socket = null;
                });

                newSocket.exceptionHandler(e -> {
                    Console.error("Socket exception for " + socketAddress(newSocket) + ": " + e.getMessage());
                    this.socket = null;
                });

                connectPromise.complete();
            } else {
                Console.error("Failed to connect socket:" + res.cause().getMessage());
                connectPromise.fail(res.cause());
            }
        });
        return connectPromise.future();
    }

    public Future<Void> sendWithResponseHandler(Buffer message,
                                                String flowKey,
                                                Handler<DiameterMessage> responseHandler) {
        if (socket == null) {
            String errorMsg = "No available sockets";
            if (flowKey != null) {
                errorMsg += " for flow " + flowKey;
            }
            Console.error(errorMsg + " (FlowKey: " + flowKey + ")");
            return Future.failedFuture(errorMsg);
        }
        responseHandlers.put(flowKey, responseHandler);

        Promise<Void> writePromise = Promise.promise();
        socket.write(message, writeOp -> {
            if (writeOp.succeeded()) {
                writePromise.complete();
                messagesSentCounter.add(1);
            } else {
                Console.error("Failed to write message to socket "
                              + socketAddress(socket)
                              + " for flow "
                              + flowKey
                              + ": "
                              + writeOp.cause().getMessage());
                responseHandlers.remove(flowKey);
                writePromise.fail(writeOp.cause());
            }
        });
        return writePromise.future();
    }

    private static String socketAddress(NetSocket socket) {
        return socket.localAddress() + "->" + socket.remoteAddress();
    }

    public Future<Void> close() {
        Console.trace("DiameterClientManager closing. Closing socket.");
        responseHandlers.clear();
        Promise<Void> closePromise = Promise.promise();

        Future<Void> socketCloseFuture = Future.succeededFuture();
        if (socket != null) {
            socketCloseFuture = socket.close();
        }

        socketCloseFuture.onComplete(ar -> {
            if (client != null) {
                client.close(clientCloseRes -> {
                    if (clientCloseRes.succeeded()) {
                        Console.trace("NetClient closed successfully.");
                    } else {
                        Console.error("NetClient close failed: " + clientCloseRes.cause());
                    }
                    closePromise.complete();
                });
            } else {
                closePromise.complete();
            }
        });
        return closePromise.future();
    }
}
