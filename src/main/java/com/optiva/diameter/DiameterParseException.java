package com.optiva.diameter;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DiameterParseException extends RuntimeException {

    private static final String NEW_LINE = "\n";
    private static final String COLUMN = ":";
    public static final String POS = "pos";

    private DiameterParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DiameterParseException create(ByteBuffer buffer, String msg, Throwable cause) {
        final int position = buffer.position();
        final int limit = buffer.limit();
        StringBuilder sb = new StringBuilder(NEW_LINE)
                .append(msg).append(COLUMN).append(NEW_LINE)
                .append(POS).append(COLUMN).append(position)
                .append(", limit").append(COLUMN).append(limit).append(NEW_LINE)
                .append("msg: ").append(NEW_LINE);
        buffer.position(0);
        final byte[] bytes = Arrays.copyOfRange(buffer.array(), 0, limit);
        final String hexAvp = DatatypeConverter.printHexBinary(bytes);
        final String[] hex = hexAvp.split("(?<=\\G.{64})");
        for (int i = 0; i < hex.length; i++) {
            sb.append(hex[i]).append(NEW_LINE);
        }
        buffer.position(position);
        return new DiameterParseException(sb.toString(), cause);
    }
}
