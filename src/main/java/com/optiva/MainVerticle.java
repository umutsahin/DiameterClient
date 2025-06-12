package com.optiva;

import com.optiva.console.Console;
import com.optiva.flows.DiameterFBC;
import com.optiva.flows.DiameterFlow;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainVerticle extends AbstractVerticle {
    private int currentTps = 0;
    private long timerId = -1;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicInteger activeFlows = new AtomicInteger(0);

    private DiameterClientVerticle clientVerticle;
    private DiameterFlowExecutor flowExecutor;
    private final Random random = new Random(); // For generating flow parameters

    // Configuration for DiameterClientVerticle
    private String diameterServerHost = "127.0.0.1";
    private int diameterServerPort = 3868;
    private int diameterSocketCount = 1; // Example: default to 1 socket

    // Configuration for DiameterFBC flow instances
    private int fbcMessageCount = 4; // Example: 3 messages per FBC flow (CCR-I, CCR-U, CCR-T)
    private int fbcRatingGroup = 16;

    @Override
    public void start(Promise<Void> startPromise) {
        // Read configuration if provided (e.g., from vertx config() or system properties)
        this.diameterServerHost = config().getString("diameter.server.host", "127.0.0.1");
        this.diameterServerPort = config().getInteger("diameter.server.port", 3868);
        this.diameterSocketCount = config().getInteger("diameter.socket.count", 1);
        this.fbcMessageCount = config().getInteger("fbc.message.count", 4);
        this.fbcRatingGroup = config().getInteger("fbc.rating.group", 16);

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
                // Initialize DiameterFlowExecutor once DiameterClientVerticle is ready
                // We need to pass the instance of the deployed verticle, not a new one.
                // However, direct instance passing is tricky if it's a different Vert.x context or for scalability.
                // For now, assuming clientVerticle is the correct, deployed instance accessible here.
                // A better way for inter-verticle communication is often the event bus.
                // But for direct method calls within the same JVM and Vert.x instance, this can work.
                flowExecutor = new DiameterFlowExecutor(clientVerticle, activeFlows);

                new Thread(this::handleStdIn).start(); // Start handling stdin commands
                startPromise.complete();
            } else {
                Console.error("Failed to deploy DiameterClientVerticle: " + deployRes.cause());
                startPromise.fail(deployRes.cause());
            }
        });
    }

    private void handleStdIn() {
        while (!shuttingDown.get()) {
            String line = Console.readLine("tps [i]/shutdown >");
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

            String command = parts[0].toLowerCase();
            if ("tps".equals(command) && parts.length == 2) {
                if (shuttingDown.get()) {
                    Console.log("Shutdown in progress. Cannot change TPS.");
                    continue;
                }
                try {
                    int newTps = Integer.parseInt(parts[1]);
                    if (newTps >= 0) {
                        adjustTps(newTps);
                    } else {
                        Console.log("TPS value must be non-negative.");
                    }
                } catch (NumberFormatException e) {
                    Console.log("Invalid TPS value: " + parts[1]);
                }
            } else if ("shutdown".equals(command)) {
                initiateShutdown();
                break;
            } else {
                Console.log("Unknown command: " + line);
            }
        }
    }

    private void adjustTps(int newTps) {
        Console.log("Adjusting TPS to " + newTps + ". Active flows: " + activeFlows.get());
        this.currentTps = newTps;

        if (timerId != -1) {
            vertx.cancelTimer(timerId);
            timerId = -1;
        }

        if (currentTps > 0 && !shuttingDown.get()) {
            if (flowExecutor == null) {
                Console.error("Flow Executor not initialized yet. Cannot start TPS.");
                return;
            }
            // Calculate interval ensuring it's at least 1ms to avoid issues with 0ms interval
            long interval = Math.max(1, 1000 / currentTps);
            timerId = vertx.setPeriodic(interval, id -> {
                if (shuttingDown.get()) {
                    vertx.cancelTimer(id);
                    return;
                }

                // Create and execute a new DiameterFBC flow
                String msisdn = "447400000" + String.format("%04d", random.nextInt(1000));
                DiameterFlow flow = new DiameterFBC(msisdn, fbcRatingGroup, fbcMessageCount);

                Console.log("MainVerticle: Triggering new flow "
                            + flow.getKey()
                            + ". Current TPS: "
                            + currentTps
                            + ". Active flows: "
                            + activeFlows.get());

                flowExecutor.executeFlow(flow).onComplete(ar -> {
                    if (ar.failed()) {
                        Console.error("Flow " + flow.getKey() + " execution failed: " + ar.cause().getMessage());
                        // Optionally, log ar.cause() for more details
                    } else {
                        // Console.log("Flow " + flow.getKey() + " completed successfully.");
                    }
                    // activeFlows counter is decremented within flowExecutor's eventually block
                    // Console.log("Active flows after completion/failure: " + activeFlows.get());
                });
            });
        } else if (currentTps == 0) {
            Console.log("TPS set to 0. No load will be generated. Active flows: " + activeFlows.get());
        }
    }

    private void initiateShutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            Console.log("Shutdown already in progress.");
            return;
        }

        Console.log("Shutdown command received. Initiating graceful shutdown...");
        if (timerId != -1) {
            vertx.cancelTimer(timerId);
        }
        Console.log("Stopped generating new flows. Waiting for " + activeFlows.get() + " active flows to complete...");

        long checkInterval = 1000;
        vertx.setPeriodic(checkInterval, checkId -> {
            int currentActive = activeFlows.get();
            if (currentActive == 0) {
                vertx.cancelTimer(checkId);
                Console.log("All active flows completed.");
                // Close DiameterClientVerticle first, then the main Vert.x instance
                // clientVerticle instance here is the one we created, deployRes.result() is its deployment ID
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
                    Console.log("DiameterClientVerticle was not deployed or ID is null, skipping undeploy.");
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
