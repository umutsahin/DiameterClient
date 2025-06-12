package com.optiva;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainVerticle extends AbstractVerticle {

    private int currentTps = 0;
    private long timerId = -1;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    // TODO: This counter should ideally be managed by DiameterFlowExecutor or a shared service
    private final AtomicInteger activeFlows = new AtomicInteger(0);

    private DiameterClientVerticle clientVerticle; // Will be deployed by MainVerticle
    // private DiameterFlowExecutor flowExecutor; // Will be initialized after clientVerticle

    @Override
    public void start(Promise<Void> startPromise) {
        clientVerticle = new DiameterClientVerticle();
        // flowExecutor = new DiameterFlowExecutor(vertx, clientVerticle);

        vertx.deployVerticle(clientVerticle, deployRes -> {
            if (deployRes.succeeded()) {
                System.out.println("DiameterClientVerticle deployed");
                // Start handling stdin commands in a separate thread after client verticle is deployed
                new Thread(this::handleStdIn).start();
                System.out.println("MainVerticle started. Enter TPS value (e.g., 'tps 100') or 'shutdown'.");
                startPromise.complete();
            } else {
                System.err.println("Failed to deploy DiameterClientVerticle: " + deployRes.cause());
                startPromise.fail(deployRes.cause());
            }
        });
    }

    private void handleStdIn() {
        Scanner scanner = new Scanner(System.in);
        while (!shuttingDown.get()) {
            if (!scanner.hasNextLine()) { // Check if there's a next line
                try {
                    Thread.sleep(100); // Wait a bit if no input is available
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            String line = scanner.nextLine();
            if (line == null && shuttingDown.get()) {
                break;
            }
            if (line == null) continue;


            String[] parts = line.trim().split("\s+");
            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }

            String command = parts[0].toLowerCase();
            if ("tps".equals(command) && parts.length == 2) {
                if (shuttingDown.get()) {
                    System.out.println("Shutdown in progress. Cannot change TPS.");
                    continue;
                }
                try {
                    int newTps = Integer.parseInt(parts[1]);
                    if (newTps >= 0) {
                        adjustTps(newTps);
                    } else {
                        System.out.println("TPS value must be non-negative.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid TPS value: " + parts[1]);
                }
            } else if ("shutdown".equals(command)) {
                initiateShutdown();
                break;
            } else {
                System.out.println("Unknown command: " + line);
                System.out.println("Available commands: 'tps <value>' or 'shutdown'");
            }
        }
        if (scanner != null) {
            scanner.close();
        }
    }

    private void adjustTps(int newTps) {
        System.out.println("Adjusting TPS to " + newTps);
        this.currentTps = newTps;

        if (timerId != -1) {
            vertx.cancelTimer(timerId);
            timerId = -1;
        }

        if (currentTps > 0 && !shuttingDown.get()) {
            long interval = 1000 / currentTps;
            timerId = vertx.setPeriodic(interval, id -> {
                if (shuttingDown.get()) {
                    vertx.cancelTimer(id); // Stop new flow executions if shutting down
                    return;
                }
                // TODO: Execute a DiameterFlow instance using DiameterFlowExecutor
                // For now, simulate flow execution and active flow counting
                activeFlows.incrementAndGet();
                System.out.println("Executing a flow (TPS: " + currentTps + "). Active flows: " + activeFlows.get());
                // Simulate flow completion
                vertx.setTimer(500, tid -> activeFlows.decrementAndGet());
            });
        } else if (currentTps == 0) {
            System.out.println("TPS set to 0. No load will be generated.");
        }
    }

    private void initiateShutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            System.out.println("Shutdown already in progress.");
            return;
        }

        System.out.println("Shutdown command received. Initiating graceful shutdown...");
        if (timerId != -1) {
            vertx.cancelTimer(timerId); // Stop generating new flows
        }
        System.out.println("Stopped generating new flows. Waiting for active flows to complete...");

        // Periodically check if active flows have completed
        long checkInterval = 1000; // Check every second
        vertx.setPeriodic(checkInterval, checkId -> {
            if (activeFlows.get() == 0) {
                vertx.cancelTimer(checkId);
                System.out.println("All active flows completed.");
                // Proceed to close Vert.x and other resources
                vertx.close(res -> {
                    if (res.succeeded()) {
                        System.out.println("MainVerticle and Vert.x shutdown complete.");
                    } else {
                        System.err.println("Failed to shutdown Vert.x: " + res.cause());
                    }
                });
            } else {
                System.out.println("Waiting for " + activeFlows.get() + " active flows to complete...");
            }
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        // This stop method is called when Vert.x is shutting down.
        // Ensure resources are released.
        shuttingDown.set(true); // Ensure shutdown flag is set
        if (timerId != -1) {
            try {
                vertx.cancelTimer(timerId);
            } catch (Exception e) {
                // Ignore if vertx is already shutting down
            }
        }
        System.out.println("MainVerticle stop method called.");
        // Actual cleanup of clientVerticle and other resources should be handled
        // by their respective stop methods or coordinated by the shutdown logic.
        stopPromise.complete();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        // Deploy MainVerticle. The MainVerticle itself will deploy DiameterClientVerticle.
        vertx.deployVerticle(new MainVerticle(), ar -> {
            if (ar.succeeded()) {
                System.out.println("MainVerticle deployed successfully.");
            } else {
                System.err.println("Failed to deploy MainVerticle: " + ar.cause());
                vertx.close(); // Close Vert.x if MainVerticle deployment fails
            }
        });
    }
}
