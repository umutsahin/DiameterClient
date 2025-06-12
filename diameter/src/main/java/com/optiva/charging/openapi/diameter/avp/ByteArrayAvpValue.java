package com.optiva.charging.openapi.diameter.avp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Base64;

public class ByteArrayAvpValue extends AvpValue<byte[]> {
    private static final byte[] EMPTY = new byte[0];

    @Override
    protected byte[] getDefaultValue() {
        return EMPTY;
    }

    @Override
    void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        final byte[] bytes = new byte[valueLength];
        buffer.get(bytes);
        skipPadding(buffer, valueLength);
        setValue(bytes);
    }

    @Override
    void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        final byte[] bytes = new byte[valueLength];
        buffer.readBytes(bytes);
        skipPadding(buffer, valueLength);
        setValue(bytes);
    }

    @Override
    int convertToBytes(ByteBuffer buffer) {
        final byte[] bytes = value != null
                             ? value
                             : EMPTY;
        buffer.put(bytes);
        addPadding(buffer, bytes.length);
        return bytes.length;
    }

    @Override
    int convertToBytes(ByteBuf buffer) {
        final byte[] bytes = value != null
                             ? value
                             : EMPTY;
        buffer.writeBytes(bytes);
        addPadding(buffer, bytes.length);
        return bytes.length;
    }

    @Override
    public String toString() {
        return "AvpValue{" + "value=" + Base64.getEncoder().encodeToString(value) + "} ";
    }
}
