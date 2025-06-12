package com.optiva.charging.openapi.diameter.avp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public class DoubleAvpValue extends AvpValue<Double> {
    private static final double ZERO = 0d;

    @Override
    protected Double getDefaultValue() {
        return ZERO;
    }

    @Override
    void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        setValue(buffer.getDouble());
    }

    @Override
    void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        setValue(buffer.readDouble());
    }

    @Override
    int convertToBytes(ByteBuffer buffer) {
        buffer.putDouble(value != null
                         ? value
                         : ZERO);
        return 8;
    }

    @Override
    int convertToBytes(ByteBuf buffer) {
        buffer.writeDouble(value != null
                           ? value
                           : ZERO);
        return 8;
    }
}
