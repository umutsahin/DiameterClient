package com.optiva.charging.openapi.diameter.avp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

public abstract class AvpValue<T> {
    protected static final ThreadLocal<byte[]> BUFFER = ThreadLocal.withInitial(() -> new byte[2048]);
    protected static final ThreadLocal<CharsetEncoder> ENCODER
            = ThreadLocal.withInitial(StandardCharsets.UTF_8::newEncoder);
    protected static final byte PADDING = (byte) 0;
    protected T value;

    protected AvpValue() {
        value = getDefaultValue();
    }

    protected AvpValue<T> setValue(T value) {
        this.value = value;
        return this;
    }

    public T getValue() {
        return value;
    }

    abstract protected T getDefaultValue();

    abstract void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode);

    abstract void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode);

    abstract int convertToBytes(ByteBuffer buffer);

    abstract int convertToBytes(ByteBuf buffer);

    protected void addPadding(ByteBuffer buffer, int valueLength) {
        final int mod = valueLength % 4;
        if (mod == 0) {
            return;
        }
        for (int i = 0; i < (4 - mod); i++) {
            buffer.put(PADDING);
        }
    }

    protected void addPadding(ByteBuf buffer, int valueLength) {
        final int mod = valueLength % 4;
        if (mod == 0) {
            return;
        }
        buffer.writeZero(4 - mod);
    }

    protected void skipPadding(ByteBuffer buffer, int valueLength) {
        int padding = valueLength % 4;
        if (padding > 0) {
            padding = 4 - padding;
            buffer.position(buffer.position() + padding);
        }
    }

    protected void skipPadding(ByteBuf buffer, int valueLength) {
        int padding = valueLength % 4;
        if (padding > 0) {
            buffer.skipBytes(4 - padding);
        }
    }

    @Override
    public String toString() {
        return "AvpValue{" + "value=" + value + '}';
    }
}
