package com.optiva;

import com.optiva.console.Console;
import com.optiva.flows.DiameterChargingFlow;
import com.optiva.handler.CommandHandler;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.optiva.handler.CommandHandler.EB_COMMAND_ACTIVE_FLOWS;
import static com.optiva.handler.CommandHandler.EB_COMMAND_RPS;

public class MainVerticle extends AbstractVerticle {
    private static final String CLASS_NAME = MainVerticle.class.getSimpleName();
    public static final JsonObject EMPTY = new JsonObject();
    private int currentRps = 0;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicInteger totalActiveFlows = new AtomicInteger(0);
    private final List<String> deployedSchedulerIds = new ArrayList<>();
    private final Map<String, Integer> activeFlowsPerScheduler = new HashMap<>();
    private String diameterServerHost;
    private int diameterServerPort;
    private int rpsPerScheduler;
    private String flowName;
    private int ratingGroup;
    private int messageCount;
    private long monitoringId = -1;
    private final Supplier<DeploymentOptions> deploymentOptions
            = () -> new DeploymentOptions().setConfig(new JsonObject().put("diameterHost", this.diameterServerHost)
                                                              .put("diameterPort", this.diameterServerPort)
                                                              .put("flowName", this.flowName)
                                                              .put("ratingGroup", this.ratingGroup)
                                                              .put("messageCount", this.messageCount));

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject server = config().getJsonObject("server", EMPTY);
        this.diameterServerHost = server.getString("host", "127.0.0.1");
        this.diameterServerPort = server.getInteger("port", 3868);
        JsonObject load = config().getJsonObject("load", EMPTY);
        this.rpsPerScheduler = load.getInteger("rps-per-scheduler", 200);
        this.flowName = load.getString("flow-name", "ims-moc");
        this.ratingGroup = load.getInteger("rating-group", 10);
        this.messageCount = load.getInteger("message-count", 4);

        Console.log(CLASS_NAME
                    + " starting. Diameter target: "
                    + diameterServerHost
                    + ":"
                    + diameterServerPort
                    + ". RPS per scheduler: "
                    + rpsPerScheduler);

        vertx.deployVerticle(new CommandHandler(), commandHandlerDeployRes -> {
            if (commandHandlerDeployRes.succeeded()) {
                Console.log("CommandHandlerVerticle deployed successfully.");
            } else {
                Console.error("Failed to deploy CommandHandlerVerticle: " + commandHandlerDeployRes.cause());
            }
        });

        vertx.eventBus().<JsonObject>consumer(EB_COMMAND_ACTIVE_FLOWS, message -> {
            String schedulerId = message.body().getString("schedulerId");
            int activeFlows = message.body().getInteger("activeFlows");
            activeFlowsPerScheduler.put(schedulerId, activeFlows);
            recalculateTotalActiveFlows();
        });

        vertx.eventBus().<JsonObject>consumer(CommandHandler.EB_COMMAND_FLOW, message -> {
            String flowName = message.body().getString("value");
            if (flowName != null && !flowName.isEmpty()) {
                if (DiameterChargingFlow.validate(flowName)) {
                    Console.warn("Setting charging flow to " + flowName);
                    this.flowName = flowName;
                } else {
                    Console.error("No such flow: " + flowName);
                }
            } else {
                Console.error("Flow command received with no flow name.");
            }
        });

        vertx.eventBus().<JsonObject>consumer(CommandHandler.EB_COMMAND_MESSAGE_COUNT, message -> {
            try {
                int mc = Integer.parseInt(message.body().getString("value"));
                if (mc > 2) {
                    Console.log("Setting message count to " + mc);
                    messageCount = mc;
                } else {
                    Console.warn("Message count value must be bigger than 2.");
                }
            } catch (NumberFormatException e) {
                Console.warn("Invalid message count value: " + message.body().getString("value"));
            }
        });

        vertx.eventBus().<JsonObject>consumer(CommandHandler.EB_COMMAND_RATING_GROUP, message -> {
            try {
                int rg = Integer.parseInt(message.body().getString("value"));
                if (rg > 0) {
                    Console.log("Setting rating group to " + rg);
                    ratingGroup = rg;
                } else {
                    Console.warn("Rating group value must be non-negative.");
                }
            } catch (NumberFormatException e) {
                Console.warn("Invalid rating group value: " + message.body().getString("value"));
            }
        });

        vertx.eventBus().<JsonObject>consumer(CommandHandler.EB_COMMAND_EXIT, message -> {
            Console.log(CLASS_NAME + " received exit command.");
            closeVertxInstance();
        });

        vertx.eventBus().<JsonObject>consumer(CommandHandler.EB_COMMAND_RPS_TOTAL, message -> {
            try {
                int newTotalRpsCmd = Integer.parseInt(message.body().getString("value"));
                if (newTotalRpsCmd >= 0) {
                    Console.log(CLASS_NAME + " received total RPS command: " + newTotalRpsCmd);
                    setRps(newTotalRpsCmd);
                } else {
                    Console.warn("Invalid total RPS value from event bus: " + newTotalRpsCmd);
                }
            } catch (NumberFormatException e) {
                Console.warn("Invalid total RPS format from event bus: " + message.body().getString("value"));
            }
        });
        scaleUp(1);
        startPromise.complete();
        Console.log(CLASS_NAME + " started successfully.");
    }

    private void recalculateTotalActiveFlows() {
        int sum = activeFlowsPerScheduler.values().stream().mapToInt(Integer::intValue).sum();
        totalActiveFlows.set(sum);
    }

    private void setRps(int rps) {
        if (shuttingDown.get()) {
            Console.log("Shutdown in progress, ignoring setRps(" + rps + ")");
            return;
        }
        if (rps < 0) {
            Console.warn("Requested total RPS cannot be negative. Received: " + rps);
            return;
        }
        if (currentRps == rps) {
            Console.warn("RPS did not change: " + rps);
            return;
        }
        Console.log("Setting total RPS to " + rps + ". RPS per scheduler: " + rpsPerScheduler);

        if (monitoringId != -1) {
            vertx.cancelTimer(monitoringId);
        }

        int numSchedulersRequired = rps == 0
                                    ? 1
                                    : Math.ceilDiv(rps, rpsPerScheduler);

        int size = deployedSchedulerIds.size();
        Console.log("Current schedulers: " + size + ", Required schedulers: " + numSchedulersRequired);

        Future<Void> future;
        if (numSchedulersRequired < size) {
            future = scaleDown(numSchedulersRequired);
        } else if (numSchedulersRequired > size) {
            future = scaleUp(numSchedulersRequired);
        } else {
            future = Future.succeededFuture();
            Console.log("no schedulers deployed.");
        }
        future.onSuccess(event -> {
            int newSize = deployedSchedulerIds.size();
            if (currentRps != rps) {
                int i = rps / newSize;
                vertx.eventBus().publish(EB_COMMAND_RPS, new JsonObject().put("value", i));
            }
            this.currentRps = rps;
            Console.log(CLASS_NAME
                        + ": RPS distribution logic finished. Target Total RPS: "
                        + currentRps
                        + ", Schedulers: "
                        + newSize);
            if (currentRps > 0) {
                monitoringId = vertx.setPeriodic(1000,
                                                 e -> Console.log("Total active flows: " + totalActiveFlows.get()));
            } else {
                monitoringId = -1;
            }
        });
    }

    private Future<Void> scaleUp(int numSchedulersRequired) {
        int numToCreate = numSchedulersRequired - deployedSchedulerIds.size();
        Console.log("Scaling up. Creating " + numToCreate + " new schedulers.");
        DeploymentOptions options = deploymentOptions.get();
        Promise<Void> promise = Promise.promise();
        List<Future<String>> futures = IntStream.range(0, numToCreate)
                .mapToObj(i -> vertx.deployVerticle(MessageScheduler.class.getName(), options))
                .toList();
        Future.all(futures).onComplete(ar -> {
            if (ar.succeeded()) {
                ar.result().<String>list().forEach(deploymentId -> {
                    deployedSchedulerIds.add(deploymentId);
                    activeFlowsPerScheduler.put(deploymentId, 0);
                    Console.log("Successfully deployed new scheduler " + deploymentId);
                });
                recalculateTotalActiveFlows();
                Console.log("Total schedulers now: " + deployedSchedulerIds.size());
                promise.complete();
            } else {
                Console.error("Failed to deploy new scheduler: " + ar.cause());
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    private Future<Void> scaleDown(int numSchedulersRequired) {
        int numToShutdown = deployedSchedulerIds.size() - numSchedulersRequired;
        Console.log("Scaling down. Shutting down " + numToShutdown + " schedulers.");
        Promise<Void> promise = Promise.promise();
        List<String> toBeRemoved = deployedSchedulerIds.subList(numToShutdown, deployedSchedulerIds.size());
        List<Future<Void>> futures = toBeRemoved.stream().map(id -> vertx.undeploy(id)).toList();
        Future.all(futures).onComplete(ar -> {
            if (ar.succeeded()) {
                toBeRemoved.forEach(deploymentId -> {
                    Console.log("Successfully un deployed scheduler " + deploymentId);
                    deployedSchedulerIds.remove(deploymentId);
                    activeFlowsPerScheduler.remove(deploymentId);
                });
                recalculateTotalActiveFlows();
                promise.complete();
            } else {
                Console.error("Failed to undeploy schedulers: " + ar.cause());
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

    private void closeVertxInstance() {
        Console.log(CLASS_NAME + " stop() called. Initiating shutdown sequence if not already started.");
        if (!shuttingDown.compareAndSet(false, true)) {
            Console.warn("Shutdown already in progress.");
        } else {
            Console.log("Shutdown command received. Initiating graceful shutdown...");
            scaleDown(0);
            long checkInterval = 1000; // ms
            vertx.setPeriodic(checkInterval, checkId -> {
                int currentActive = totalActiveFlows.get();
                if (currentActive == 0) {
                    vertx.cancelTimer(checkId);
                    Console.log("All active flows completed. Undeploying any remaining schedulers.");
                    closeVertx();
                } else {
                    Console.log("Waiting for " + currentActive + " active flows to complete...");
                }
            });
        }
    }

    private void closeVertx() {
        Console.log("Closing Vert.x instance.");
        vertx.close(res -> {
            if (res.succeeded()) {
                Console.log(CLASS_NAME + " and Vert.x shutdown complete.");
            } else {
                Console.error("Failed to shutdown Vert.x: " + res.cause());
            }
        });
    }

    public static void main(String[] args) {
        Console.debug("Starting application...");
        Vertx vertx = Vertx.vertx();
        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig().onSuccess(config -> {
            DeploymentOptions mainOptions = new DeploymentOptions().setConfig(config);
            vertx.deployVerticle(new MainVerticle(), mainOptions, ar -> {
                if (ar.succeeded()) {
                    Console.log(CLASS_NAME + " deployed successfully from main().");
                } else {
                    Console.error("Failed to deploy MainVerticle from main(): " + ar.cause(), ar.cause());
                    vertx.close();
                }
            });
        });

    }
}
