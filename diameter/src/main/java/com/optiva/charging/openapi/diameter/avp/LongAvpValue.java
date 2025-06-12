package com.optiva.charging.openapi.diameter.avp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public class LongAvpValue extends AvpValue<Long> {
    private static final long ZERO = 0L;

    @Override
    protected Long getDefaultValue() {
        return ZERO;
    }

    @Override
    void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        setValue(buffer.getLong());
    }

    @Override
    void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        setValue(buffer.readLong());
    }

    @Override
    int convertToBytes(ByteBuffer buffer) {
        buffer.putLong(value != null
                       ? value
                       : ZERO);
        return 8;
    }

    @Override
    int convertToBytes(ByteBuf buffer) {
        buffer.writeLong(value != null
                         ? value
                         : ZERO);
        return 8;
    }
}
