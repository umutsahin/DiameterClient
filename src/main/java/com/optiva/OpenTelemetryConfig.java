package com.optiva;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

public class OpenTelemetryConfig {

    private static Meter meter;
    private static LongCounter messagesSentCounter;
    private static DoubleHistogram messageLatencyHistogram;
    private static Tracer tracer;
    private static Logger otelLogger; // Add this

    public static void initialize() {
        AutoConfiguredOpenTelemetrySdk autoConfiguredSdk = AutoConfiguredOpenTelemetrySdk.initialize();
        OpenTelemetrySdk sdk = autoConfiguredSdk.getOpenTelemetrySdk();
        GlobalOpenTelemetry.set(sdk); // This sets up GlobalTracerProvider, GlobalMeterProvider

        // It's important that GlobalLoggerProvider.set() is called by the SDK,
        // which AutoConfiguredOpenTelemetrySdk should do.
        // We then get the logger from the global provider.

        meter = GlobalOpenTelemetry.getMeter("com.optiva.DiameterClient");
        tracer = GlobalOpenTelemetry.getTracer("com.optiva.MessageScheduler");
        // Get LoggerProvider from the SDK instance, then get the logger
        otelLogger = sdk.getSdkLoggerProvider().get("com.optiva.Console");


        messagesSentCounter = meter
                .counterBuilder("messages.sent")
                .setDescription("Counts the number of messages sent")
                .setUnit("1")
                .build();

        messageLatencyHistogram = meter
                .histogramBuilder("message.latency")
                .setDescription("Measures the end-to-end latency of messages")
                .setUnit("ms")
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(sdk::close));
    }

    public static LongCounter getMessagesSentCounter() {
        if (messagesSentCounter == null) {
            if (meter == null) { // Should be initialized by initialize()
                meter = GlobalOpenTelemetry.getMeter("com.optiva.DiameterClient");
            }
            messagesSentCounter = meter.counterBuilder("messages.sent").setDescription("Counts the number of messages sent").setUnit("1").build();
        }
        return messagesSentCounter;
    }

    public static DoubleHistogram getMessageLatencyHistogram() {
        if (messageLatencyHistogram == null) {
            if (meter == null) { // Should be initialized by initialize()
                 meter = GlobalOpenTelemetry.getMeter("com.optiva.DiameterClient");
            }
            messageLatencyHistogram = meter.histogramBuilder("message.latency").setDescription("Measures the end-to-end latency of messages").setUnit("ms").build();
        }
        return messageLatencyHistogram;
    }

    public static Tracer getTracer() {
        if (tracer == null) {
            // Fallback, should be initialized by initialize()
            tracer = GlobalOpenTelemetry.getTracer("com.optiva.MessageScheduler");
        }
        return tracer;
    }

    public static Logger getOtelLogger() { // Add this method
        if (otelLogger == null) {
            // This case should ideally not happen if initialize() is called first.
            // Returning null is an option if the caller handles it (which Console.java does).
            // Alternatively, throw new IllegalStateException("OpenTelemetryConfig not initialized or logger not available.");
            System.err.println("Warning: OpenTelemetryConfig.getOtelLogger() called when otelLogger is null. Ensure initialize() was called and succeeded.");
        }
        return otelLogger;
    }
}
