## OpenTelemetry Integration

This application integrates OpenTelemetry to provide observability into its operations. This allows for monitoring and
tracing of application behavior, which is crucial for understanding performance and diagnosing issues in distributed
environments.

### Telemetry Data Collected

The following telemetry data is currently being collected:

* **Metrics**:
    * `messages.sent` (Counter): Counts the total number of messages successfully sent by the Diameter client. Unit:
      `1` (message).
    * `message.latency` (Histogram): Measures the end-to-end latency of messages from the time they are about to be sent
      until a response is received or a failure occurs. Unit: `ms` (milliseconds).
* **Traces**:
    * Traces are generated for the processing of messages within the `MessageScheduler`. This includes spans covering
      the queuing, sending, and response handling for each message, providing visibility into the lifecycle of message
      processing.
    * Spans are automatically associated with their parent span if a trace context is propagated.
* **Logs**:
    * Debug logs emitted via the application's `Console.debug()` method are also exported through OpenTelemetry. These
      logs are automatically correlated with the active trace and span when available.

### Configuration

The OpenTelemetry SDK is configured using the `opentelemetry-sdk-extension-autoconfigure` module, which allows
configuration primarily through environment variables or system properties. Key variables include:

* `OTEL_SERVICE_NAME`: Defines the service name that will appear in your telemetry backend. (e.g.,
  `OTEL_SERVICE_NAME=DiameterClientApp`)
* `OTEL_EXPORTER_OTLP_ENDPOINT`: Specifies the OTLP endpoint where telemetry data (metrics, traces, logs) will be sent.
  This could be an OpenTelemetry Collector or a compatible backend. (e.g., `http://localhost:4317` for gRPC or
  `http://localhost:4318` for HTTP/protobuf - ensure you use the correct path for traces, metrics, logs if your exporter
  requires it e.g. `http://localhost:4318/v1/traces`).
* `OTEL_TRACES_EXPORTER`: Can be set to `otlp`, `jaeger`, `zipkin`, `logging`, etc. Defaults often include `otlp`.
* `OTEL_METRICS_EXPORTER`: Can be set to `otlp`, `prometheus`, `logging`, etc. Defaults often include `otlp`.
* `OTEL_LOGS_EXPORTER`: To enable log exporting, set this to `otlp` or `logging`. If not set, logs might not be
  exported. (e.g., `OTEL_LOGS_EXPORTER=otlp`)
* `OTEL_PROPAGATORS`: Configures context propagation (e.g., `tracecontext,baggage`).

Refer to
the [OpenTelemetry SDK Autoconfiguration documentation](https://opentelemetry.io/docs/languages/java/configuration/) for
a complete list of configuration options.

### Example Usage with OTLP Exporter

To run the application and send telemetry data to an OTLP-compatible backend (like an OpenTelemetry Collector) running
on `http://localhost:4317` (gRPC) or `http://localhost:4318` (HTTP), you can set the environment variables before
running the application.

**Example using gRPC endpoint (port 4317 is common for gRPC):**

```bash
export OTEL_SERVICE_NAME="DiameterClientApp"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"
export OTEL_TRACES_EXPORTER="otlp"
export OTEL_METRICS_EXPORTER="otlp"
export OTEL_LOGS_EXPORTER="otlp" # Ensure logs are exported

java -jar build/libs/DiameterClientServer-1.0-SNAPSHOT.jar
```

**Example using HTTP/protobuf endpoint (port 4318 is common for HTTP):**
Note: The OTLP HTTP exporter often requires separate paths for signals. The `autoconfigure` module attempts to handle
this, but for older versions or specific collector setups, you might need to be more explicit if just the base endpoint
doesn't work for all signals. For instance, some setups might implicitly expect `/v1/traces`, `/v1/metrics`, `/v1/logs`
appended to the endpoint. `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`, `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT`,
`OTEL_EXPORTER_OTLP_LOGS_ENDPOINT` can be used for more specific URLs.

```bash
export OTEL_SERVICE_NAME="DiameterClientApp"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4318" # General endpoint
# Or more specific if needed:
# export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT="http://localhost:4318/v1/traces"
# export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT="http://localhost:4318/v1/metrics"
# export OTEL_EXPORTER_OTLP_LOGS_ENDPOINT="http://localhost:4318/v1/logs"
export OTEL_TRACES_EXPORTER="otlp"
export OTEL_METRICS_EXPORTER="otlp"
export OTEL_LOGS_EXPORTER="otlp"

java -jar build/libs/DiameterClientServer-1.0-SNAPSHOT.jar
```

Ensure an OpenTelemetry Collector or a compatible observability backend is running and configured to receive data on the
specified endpoint.
The application initializes OpenTelemetry at startup via `OpenTelemetryConfig.initialize()`.
The `opentelemetry-sdk-extension-autoconfigure` module handles the discovery and setup of exporters and other SDK
components based on the classpath and environment variables/system properties.
