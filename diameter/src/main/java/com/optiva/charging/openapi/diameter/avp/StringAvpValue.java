package com.optiva.charging.openapi.diameter.avp;

import com.optiva.charging.openapi.diameter.exception.DiameterParseException;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StringAvpValue extends AvpValue<String> {
    private static final String EMPTY = "";

    @Override
    protected String getDefaultValue() {
        return EMPTY;
    }

    @Override
    public void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        final int oldLimit = buffer.limit();
        buffer.limit(buffer.position() + valueLength);
        try {
            buffer.get(BUFFER.get(), 0, valueLength);
            setValue(new String(BUFFER.get(), 0, valueLength, UTF_8));
        } catch (Exception e) {
            throw DiameterParseException.create(buffer, "Unable to parse avp value: " + avpCode.toString(), e);
        } finally {
            buffer.limit(oldLimit);
            skipPadding(buffer, valueLength);
        }
    }

    @Override
    public void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        try {
            setValue(buffer.readCharSequence(valueLength, UTF_8).toString());
        } catch (Exception e) {
            throw DiameterParseException.create(buffer, "Unable to parse avp value: " + avpCode.toString(), e);
        } finally {
            skipPadding(buffer, valueLength);
        }
    }

    @Override
    public int convertToBytes(ByteBuffer buffer) {
        int length = buffer.position();
        CharBuffer charBuffer = CharBuffer.wrap(value != null
                                                ? value
                                                : EMPTY);
        ENCODER.get().encode(charBuffer, buffer, false);
        length = buffer.position() - length;
        addPadding(buffer, length);
        return length;
    }

    @Override
    public int convertToBytes(ByteBuf buffer) {
        int length = buffer.writeCharSequence(value, ENCODER.get().charset());
        addPadding(buffer, length);
        return length;
    }
}
