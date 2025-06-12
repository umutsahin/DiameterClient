package com.optiva.flows;

import io.vertx.core.buffer.Buffer; // Changed import
import java.util.concurrent.ThreadLocalRandom;

public interface DiameterFlow {
    ThreadLocal<Buffer> BUFFER = ThreadLocal.withInitial(() -> Buffer.buffer(8192)); // Changed type and initialization
    ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    Buffer getNextMessage(); // Changed return type

    String getKey();

    DiameterFlow restart();
}
