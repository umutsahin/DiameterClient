package com.optiva.flows;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;

import java.util.concurrent.ThreadLocalRandom;

public interface DiameterFlow {
    ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    Buffer getNextMessage(); // Changed return type

    String getKey();

    DiameterFlow restart();

    default Buffer writeMessageToBuffer(DiameterMessage dm) {
        BufferImpl buffer = (BufferImpl) Buffer.buffer(8192);
        ByteBuf byteBuf = buffer.byteBuf();
        dm.convertToByteBuf(byteBuf);
        return buffer;
    }
}
