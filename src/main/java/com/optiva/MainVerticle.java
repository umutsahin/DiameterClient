package com.optiva;

import com.optiva.console.Console;
import com.optiva.flows.DiameterChargingFlow;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainVerticle extends AbstractVerticle {
    private int currentTps = 0;
    private long timerId = -1;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicInteger activeFlows = new AtomicInteger(0);

    private DiameterClientVerticle clientVerticle;
    private MessageScheduler messageScheduler;

    // Configuration for DiameterClientVerticle
    private String diameterServerHost = "127.0.0.1";
    private int diameterServerPort = 3868;
    private int diameterSocketCount = 1; // Example: default to 1 socket

    @Override
    public void start(Promise<Void> startPromise) {
        // Read configuration if provided (e.g., from vertx config() or system properties)
        this.diameterServerHost = config().getString("diameter.server.host", "127.0.0.1");
        this.diameterServerPort = config().getInteger("diameter.server.port", 3868);
        this.diameterSocketCount = config().getInteger("diameter.socket.count", 1);

        clientVerticle = new DiameterClientVerticle();
        DeploymentOptions clientOptions = new DeploymentOptions().setConfig(new JsonObject().put("serverHost",
                                                                                                 diameterServerHost)
                                                                                    .put("serverPort",
                                                                                         diameterServerPort)
                                                                                    .put("socketCount",
                                                                                         diameterSocketCount));

        vertx.deployVerticle(clientVerticle, clientOptions, deployRes -> {
            if (deployRes.succeeded()) {
                Console.log("DiameterClientVerticle deployed successfully with ID: " + deployRes.result());
                this.messageScheduler = new MessageScheduler(vertx,
                                                             clientVerticle,
                                                             activeFlows);
                Console.log("MessageScheduler initialized.");
                new Thread(this::handleStdIn, "Console-Thread").start();
                startPromise.complete();
            } else {
                Console.error("Failed to deploy DiameterClientVerticle: " + deployRes.cause());
                startPromise.fail(deployRes.cause());
            }
        });
    }

    private void handleStdIn() {
        while (!shuttingDown.get()) {
            String line = Console.readPrompt();
            if (line == null && shuttingDown.get()) {
                break;
            }
            if (line == null) {
                continue;
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }

            switch (parts[0].toLowerCase()) {
                case "flow" -> {
                    if (DiameterChargingFlow.validate(parts[1])) {
                        Console.warn("Now simulator will use " + parts[1] + " flow");
                        messageScheduler.flow(parts[1]);
                    } else {
                        Console.error("No such flow: " + parts[1]);
                    }
                }
                case "rating-group" -> {
                    try {
                        int rg = Integer.parseInt(parts[1]);
                        if (rg > 0) {
                            messageScheduler.setRatingGroup(rg);
                        } else {
                            Console.warn("Rating group value must be non-negative.");
                        }
                    } catch (NumberFormatException e) {
                        Console.warn("Invalid rating group value: " + parts[1]);
                    }
                }
                case "message-count" -> {
                    try {
                        int mc = Integer.parseInt(parts[1]);
                        if (mc > 2) {
                            messageScheduler.setMessageCount(mc);
                        } else {
                            Console.warn("Message count value must be bigger than 2.");
                        }
                    } catch (NumberFormatException e) {
                        Console.warn("Invalid message count value: " + parts[1]);
                    }
                }
                case "single" -> {
                    if (messageScheduler.isNotFlowSet()) {
                        Console.error("You must set a flow first");
                        continue;
                    }
                    Console.log("Starting single flow...");
                    messageScheduler.singleFlow();
                }
                case "rps" -> {
                    if (shuttingDown.get()) {
                        Console.log("Shutdown in progress. Cannot change RPS.");
                        continue;
                    }
                    if (parts.length != 2) {
                        Console.error("rps command requires 2 arguments");
                        continue;
                    }
                    if (messageScheduler.isNotFlowSet()) {
                        Console.error("You must set a flow first");
                        continue;
                    }
                    try {
                        int newTps = Integer.parseInt(parts[1]);
                        if (newTps >= 0) {
                            adjustRps(newTps);
                        } else {
                            Console.warn("RPS value must be non-negative.");
                        }
                    } catch (NumberFormatException e) {
                        Console.warn("Invalid RPS value: " + parts[1]);
                    }
                }
                case "log-level" -> {
                    if (parts.length != 2) {
                        Console.error("debug command requires 2 arguments");
                        continue;
                    }
                    try {
                        int level = Integer.parseInt(parts[1]);
                        if (level >= 0) {
                            Console.setLevel(level);
                        } else {
                            Console.warn("debug value must be non-negative.");
                        }
                    } catch (NumberFormatException e) {
                        Console.warn("Invalid debug value: " + parts[1]);
                    }
                }
                case "exit" -> initiateShutdown();
                default -> Console.error("Unknown command: " + line);
            }
        }
    }

    private void adjustRps(int newTps) {
        if (currentTps == newTps) {
            Console.log("RPS did not change. Active flows: " + activeFlows.get());
            return;
        }
        this.messageScheduler.setRps(newTps);
        this.currentTps = newTps;
    }

    private void initiateShutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            Console.warn("Shutdown already in progress.");
            return;
        }

        Console.log("Shutdown command received. Initiating graceful shutdown...");
        messageScheduler.setRps(0);
        Console.log("Stopped generating new flows. Waiting for " + activeFlows.get() + " active flows to complete...");

        long checkInterval = 1000;
        vertx.setPeriodic(checkInterval, checkId -> {
            int currentActive = activeFlows.get();
            if (currentActive == 0) {
                vertx.cancelTimer(checkId);
                if (timerId != -1) {
                    vertx.cancelTimer(timerId);
                }
                String deploymentId = clientVerticle.deploymentID();
                if (deploymentId != null) {
                    vertx.undeploy(deploymentId, undeployRes -> {
                        if (undeployRes.succeeded()) {
                            Console.log("DiameterClientVerticle undeployed.");
                        } else {
                            Console.error("Failed to undeploy DiameterClientVerticle: " + undeployRes.cause());
                        }
                        closeVertxInstance();
                    });
                } else {
                    Console.error("DiameterClientVerticle was not deployed or ID is null, skipping undeploy.");
                    closeVertxInstance();
                }
            } else {
                Console.log("Waiting for " + currentActive + " active flows to complete...");
            }
        });
    }

    private void closeVertxInstance() {
        Console.log("Closing Vert.x instance.");
        vertx.close(res -> {
            if (res.succeeded()) {
                Console.log("MainVerticle and Vert.x shutdown complete.");
            } else {
                Console.error("Failed to shutdown Vert.x: " + res.cause());
            }
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        shuttingDown.set(true);
        if (timerId != -1) {
            try {
                vertx.cancelTimer(timerId);
            } catch (Exception e) {
                // Ignore
            }
        }
        Console.log("MainVerticle stop method called. Active flows: " + activeFlows.get());
        // Wait for active flows to complete if any, then complete promise.
        // This is mostly handled by initiateShutdown, but as a fallback:
        if (activeFlows.get() == 0) {
            stopPromise.complete();
        } else {
            // Poll until active flows are done, or timeout.
            long shutdownTimeout = 30000; // 30 seconds
            long pollInterval = 500; // ms
            AtomicInteger polls = new AtomicInteger(0); // Renamed to avoid conflict with class member
            vertx.setPeriodic(pollInterval, shutdownPollId -> {
                if (activeFlows.get() == 0) {
                    vertx.cancelTimer(shutdownPollId);
                    Console.log("MainVerticle stop: All flows completed.");
                    stopPromise.complete();
                } else if (polls.incrementAndGet() * pollInterval > shutdownTimeout) {
                    vertx.cancelTimer(shutdownPollId);
                    Console.error("MainVerticle stop: Timeout waiting for active flows. "
                                  + activeFlows.get()
                                  + " remaining.");
                    stopPromise.fail("Timeout waiting for active flows in MainVerticle stop.");
                }
            });
        }
    }

    public static void main(String[] args) {
        Console.debug("Starting application...");
        Vertx vertx = Vertx.vertx();
        DeploymentOptions mainOptions = new DeploymentOptions().setConfig(new JsonObject()
                                                                          // Example: Pass configuration to MainVerticle if needed from main()
                                                                          // .put("diameter.server.host", "custom.host.com")
                                                                          // .put("diameter.server.port", 12345)
                                                                          // .put("diameter.socket.count", 5)
                                                                          // .put("fbc.message.count", 3)
        );
        vertx.deployVerticle(new MainVerticle(), mainOptions, ar -> {
            if (ar.succeeded()) {
                Console.log("MainVerticle deployed successfully from main().");
            } else {
                Console.error("Failed to deploy MainVerticle from main(): " + ar.cause());
                vertx.close();
            }
        });
    }
}
