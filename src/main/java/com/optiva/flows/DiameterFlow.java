package com.optiva.flows;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;

import java.util.concurrent.ThreadLocalRandom;

import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SESSION_ID;

public interface DiameterFlow {
    ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    Buffer getNextMessage();

    boolean isInitialized();

    DiameterFlow terminate();

    String getKey();

    DiameterFlow restart();

    default Buffer writeMessageToBuffer(DiameterMessage dm) {
        BufferImpl buffer = (BufferImpl) Buffer.buffer(8192);
        ByteBuf byteBuf = buffer.byteBuf();
        dm.convertToByteBuf(byteBuf);
        return buffer;
    }

    static String getKey(DiameterMessage dm) {
        return switch (dm.getHeader().getCommandCode()) {
            case CC -> dm.getAvpValue(SESSION_ID);
            case CE -> "ce";
            default -> throw new IllegalStateException("Unexpected value: " + dm.getHeader().getCommandCode());
        };
    }
}
