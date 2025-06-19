package com.optiva.handler;

import com.optiva.console.Console;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class CommandHandler extends AbstractVerticle {
    private static final String CLASS_NAME = CommandHandler.class.getName();
    public static final String EB_COMMAND_PREFIX = "command.";
    public static final String EB_COMMAND_FLOW = EB_COMMAND_PREFIX + "flow";
    public static final String EB_COMMAND_RATING_GROUP = EB_COMMAND_PREFIX + "rating-group";
    public static final String EB_COMMAND_MESSAGE_COUNT = EB_COMMAND_PREFIX + "message-count";
    public static final String EB_COMMAND_SINGLE = EB_COMMAND_PREFIX + "single";
    public static final String EB_COMMAND_ACTIVE_FLOWS = EB_COMMAND_PREFIX + "activeFlows";
    public static final String EB_COMMAND_RPS = EB_COMMAND_PREFIX + "rps";
            // Retained for individual scheduler if needed later
    public static final String EB_COMMAND_RPS_TOTAL = EB_COMMAND_PREFIX + "rps-total";
    public static final String EB_COMMAND_EXIT = EB_COMMAND_PREFIX + "exit";

    private Thread consoleThread;
    private volatile boolean running = true;

    @Override
    public void start(Promise<Void> startPromise) {
        consoleThread = new Thread(this::handleConsoleInput, "command-handler-thread");
        consoleThread.start();
        Console.log(CLASS_NAME + " started and listening for commands.");
        startPromise.complete();
    }

    private void handleConsoleInput() {
        while (running) {
            String line = Console.readPrompt();
            if (line == null) {
                continue;
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }
            String command = parts[0].toLowerCase();
            JsonObject arguments = new JsonObject();
            if (parts.length > 1) {
                arguments.put("value", parts[1]);
            }
            switch (command) {
                case "flow":
                    if (parts.length > 1) {
                        vertx.eventBus().publish(EB_COMMAND_FLOW, arguments);
                    } else {
                        Console.error(CLASS_NAME + " Flow command requires an argument.");
                    }
                    break;
                case "rating-group":
                    vertx.eventBus().publish(EB_COMMAND_RATING_GROUP, arguments);
                    break;
                case "message-count":
                    vertx.eventBus().publish(EB_COMMAND_MESSAGE_COUNT, arguments);
                    break;
                case "single":
                    vertx.eventBus().publish(EB_COMMAND_SINGLE, new JsonObject()); // No arguments needed
                    break;
                case "rps": // This command now controls the TOTAL RPS for the system
                    vertx.eventBus().publish(EB_COMMAND_RPS_TOTAL, arguments);
                    break;
                case "log-level":
                    try {
                        int level = Integer.parseInt(parts[1]);
                        if (level >= 0) {
                            Console.setLevel(level);
                            Console.log(CLASS_NAME + " Log level set to: " + level);
                        } else {
                            Console.warn(CLASS_NAME + " Log level value must be non-negative.");
                        }
                    } catch (NumberFormatException e) {
                        Console.warn(CLASS_NAME + " Invalid log level value: " + parts[1]);
                    }
                    break;
                case "exit":
                    running = false; // Stop the loop
                    vertx.eventBus().publish(EB_COMMAND_EXIT, new JsonObject());
                    break;
                default:
                    Console.error(CLASS_NAME + " Unknown command: " + line);
                    break;
            }
        }
        Console.log(CLASS_NAME + " input loop stopped.");
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        running = false;
        if (consoleThread != null) {
            consoleThread.interrupt();
            try {
                consoleThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Console.warn(CLASS_NAME + " stop: Interrupted while waiting for console thread to finish.");
            }
        }
        Console.log(CLASS_NAME + " stopped.");
        stopPromise.complete();
    }
}
