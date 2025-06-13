package com.optiva;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.io.IOException;
import java.net.Socket;

public class OpenTelemetryConfig {

    public static final LongCounter messagesSentCounter;
    public static final DoubleHistogram messageLatencyHistogram;
    public static final Tracer tracer;
    public static final Logger otelLogger; // Add this

    static {
        boolean enabled;
        try (Socket localhost = new Socket("localhost", 4317)) {
            enabled = localhost.isConnected();
        } catch (IOException e) {
            enabled = false;
        }
        if (enabled) {
            Sampler sampler = Sampler.traceIdRatioBased(0.1);
            Resource newResource = Resource.create(Attributes.of(AttributeKey.stringKey("service.name"),
                                                                 "Diameter-Load-Simulator",
                                                                 AttributeKey.stringKey("traces.exporter"),
                                                                 String.valueOf(enabled)));
            AutoConfiguredOpenTelemetrySdk.builder()
                    .setResultAsGlobal()
                    .addResourceCustomizer((resource, configProps) -> resource.merge(newResource))
                    .addTracerProviderCustomizer((builder, config) -> builder.setSampler(sampler))
                    .build();
        } else {
            GlobalOpenTelemetry.set(OpenTelemetry.noop());
        }

        Meter meter = GlobalOpenTelemetry.getMeter("com.optiva.DiameterClient");
        tracer = GlobalOpenTelemetry.getTracer("com.optiva.MessageScheduler");
        otelLogger = GlobalOpenTelemetry.get().getLogsBridge().get("com.optiva.Console");
        messagesSentCounter = meter.counterBuilder("messages.sent")
                .setDescription("Counts the number of messages sent")
                .setUnit("1")
                .build();
        messageLatencyHistogram = meter.histogramBuilder("message.latency")
                .setDescription("Measures the end-to-end latency of messages")
                .setUnit("ms")
                .build();
    }
}
