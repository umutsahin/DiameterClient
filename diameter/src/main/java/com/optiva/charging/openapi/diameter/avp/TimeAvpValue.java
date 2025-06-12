package com.optiva.charging.openapi.diameter.avp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeAvpValue extends AvpValue<ZonedDateTime> {
    private static final ZonedDateTime DEFAULT = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

    @Override
    protected ZonedDateTime getDefaultValue() {
        return DEFAULT;
    }

    @Override
    void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        if (valueLength == 4) {
            int signedInt = buffer.getInt();
            long unsignedLong = Integer.toUnsignedLong(signedInt);
            setValue(DEFAULT.plusSeconds(unsignedLong));
        } else if (valueLength == 8) {
            throw new RuntimeException("DiameterTimeAvp");
        } else if (valueLength == 16) {
            throw new RuntimeException("DiameterTimeAvp");
        }
    }

    @Override
    void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        if (valueLength == 4) {
            int signedInt = buffer.readInt();
            long unsignedLong = Integer.toUnsignedLong(signedInt);
            setValue(DEFAULT.plusSeconds(unsignedLong));
        } else if (valueLength == 8) {
            throw new RuntimeException("DiameterTimeAvp");
        } else if (valueLength == 16) {
            throw new RuntimeException("DiameterTimeAvp");
        }
    }

    @Override
    int convertToBytes(ByteBuffer buffer) {
        buffer.putInt(value != null
                      ? (int) (Duration.between(DEFAULT, value).toSeconds())
                      : 0);
        return 4;
    }

    @Override
    int convertToBytes(ByteBuf buffer) {
        buffer.writeInt(value != null
                        ? (int) (Duration.between(DEFAULT, value).toSeconds())
                        : 0);
        return 4;
    }
}
