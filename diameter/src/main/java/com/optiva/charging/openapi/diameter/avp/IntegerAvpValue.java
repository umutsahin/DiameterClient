package com.optiva.charging.openapi.diameter.avp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public class IntegerAvpValue extends AvpValue<Integer> {
    private final int ZERO = 0;

    @Override
    protected Integer getDefaultValue() {
        return ZERO;
    }

    @Override
    void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        setValue(buffer.getInt());
    }

    @Override
    void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        setValue(buffer.readInt());
    }

    @Override
    int convertToBytes(ByteBuffer buffer) {
        buffer.putInt(value != null
                      ? value
                      : ZERO);
        return 4;
    }

    @Override
    int convertToBytes(ByteBuf buffer) {
        buffer.writeInt(value != null
                        ? value
                        : ZERO);
        return 4;
    }
}
