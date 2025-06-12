package com.optiva.charging.openapi.diameter.exception;

import com.optiva.charging.openapi.diameter.common.DatatypeConverter;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.optiva.charging.openapi.diameter.common.enumeration.ResultCode.DIAMETER_INVALID_AVP_LENGTH;

public class DiameterParseException extends DiameterException {

    private static final String NEW_LINE = "\n";
    private static final String COLUMN = ":";
    public static final String POS = "pos";

    private DiameterParseException(String message, Throwable cause) {
        super(message, cause, DIAMETER_INVALID_AVP_LENGTH.code());
    }

    public static DiameterParseException create(ByteBuffer buffer, String msg, Throwable cause) {
        final int position = buffer.position();
        final int limit = buffer.limit();
        StringBuilder sb = new StringBuilder(NEW_LINE).append(msg)
                .append(COLUMN)
                .append(NEW_LINE)
                .append(POS)
                .append(COLUMN)
                .append(position)
                .append(", limit")
                .append(COLUMN)
                .append(limit)
                .append(NEW_LINE)
                .append("msg: ")
                .append(NEW_LINE);
        buffer.position(0);
        code(limit, sb, buffer.array());
        buffer.position(position);
        return new DiameterParseException(sb.toString(), cause);
    }

    public static DiameterParseException create(ByteBuf buffer, String msg, Throwable cause) {
        buffer.markReaderIndex();
        final int limit = buffer.writerIndex();
        StringBuilder sb = new StringBuilder(NEW_LINE).append(msg)
                .append(COLUMN)
                .append(NEW_LINE)
                .append(POS)
                .append(COLUMN)
                .append(buffer.readerIndex())
                .append(", limit")
                .append(COLUMN)
                .append(limit)
                .append(NEW_LINE)
                .append("msg: ")
                .append(NEW_LINE);
        buffer.readerIndex(0);
        code(limit, sb, buffer.array());
        buffer.resetReaderIndex();
        return new DiameterParseException(sb.toString(), cause);
    }

    private static void code(int limit, StringBuilder sb, byte[] array) {
        final byte[] bytes = Arrays.copyOfRange(array, 0, limit);
        final String hexAvp = DatatypeConverter.printHexBinary(bytes);
        sb.append(hexAvp);
    }
}
