package com.optiva.flows;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.console.Console;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;

import java.util.concurrent.ThreadLocalRandom;

import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ERROR_MESSAGE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.RESULT_CODE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SESSION_ID;

public abstract class DiameterFlow {
    protected static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    public abstract Buffer getNextMessage();

    public final void processResponse(DiameterMessage responseMessage) {
        Integer resultCode = responseMessage.getAvpValue(RESULT_CODE);
        boolean isSuccess = resultCode != null && (resultCode >= 2000 && resultCode < 3000);

        if (!isSuccess) {
            String errorMsg = responseMessage.getAvpValue(ERROR_MESSAGE);
            Console.error(this + " failed. Result-Code: " + resultCode + ". Message: " + errorMsg);
            terminateFlow();
            return;
        }
        iterateFlow();
    }

    public abstract boolean isInProgress();

    public abstract void iterateFlow();

    public abstract void terminateFlow();

    public abstract String getKey();

    public abstract DiameterFlow restart();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getKey() + ")";
    }

    public static Buffer writeMessageToBuffer(DiameterMessage dm) {
        BufferImpl buffer = (BufferImpl) Buffer.buffer(8192);
        ByteBuf byteBuf = buffer.byteBuf();
        dm.convertToByteBuf(byteBuf);
        return buffer;
    }

    public static String getKey(DiameterMessage dm) {
        return switch (dm.getHeader().getCommandCode()) {
            case CC -> dm.getAvpValue(SESSION_ID);
            case CE -> "ce";
            default -> throw new IllegalStateException("Unexpected value: " + dm.getHeader().getCommandCode());
        };
    }
}
