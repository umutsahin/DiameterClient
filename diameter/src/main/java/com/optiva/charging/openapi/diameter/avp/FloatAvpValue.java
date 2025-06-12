package com.optiva.charging.openapi.diameter.avp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public class FloatAvpValue extends AvpValue<Float> {
    public static final float ZERO = 0f;

    @Override
    protected Float getDefaultValue() {
        return ZERO;
    }

    @Override
    void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        setValue(buffer.getFloat());
    }

    @Override
    void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        setValue(buffer.readFloat());
    }

    @Override
    int convertToBytes(ByteBuffer buffer) {
        buffer.putFloat(value != null
                        ? value
                        : ZERO);
        return 4;
    }

    @Override
    int convertToBytes(ByteBuf buffer) {
        buffer.writeFloat(value != null
                          ? value
                          : ZERO);
        return 4;
    }
}
