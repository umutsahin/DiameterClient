package com.optiva.flows;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public interface DiameterFlow {
    ThreadLocal<ByteBuffer> BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocate(8192));
    ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    ByteBuffer getNextMessage();

    String getKey();

    DiameterFlow restart();
}
