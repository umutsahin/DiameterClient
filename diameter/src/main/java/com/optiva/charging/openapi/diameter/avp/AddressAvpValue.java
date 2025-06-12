package com.optiva.charging.openapi.diameter.avp;

import com.optiva.charging.openapi.diameter.exception.DiameterParseException;
import io.netty.buffer.ByteBuf;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class AddressAvpValue extends AvpValue<InetAddress> {
    private static final Inet4Address DEFAULT = null;
    private static final byte[] EMPTY = new byte[0];

    @Override
    protected InetAddress getDefaultValue() {
        return DEFAULT;
    }

    @Override
    void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        short type = buffer.getShort();
        final byte[] bytes = new byte[valueLength - 2];
        buffer.get(bytes);
        skipPadding(buffer, valueLength);
        try {
            if (type == 1) {
                setValue(Inet4Address.getByAddress(bytes));
            } else if (type == 2) {
                setValue(Inet6Address.getByAddress(bytes));
            } else {
                throw DiameterParseException.create(buffer, "Unable to find address type", new RuntimeException());
            }
        } catch (UnknownHostException e) {
            throw DiameterParseException.create(buffer, "Unable to parse Address", e);
        }
    }

    @Override
    void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        short type = buffer.readShort();
        final byte[] bytes = new byte[valueLength - 2];
        buffer.readBytes(bytes);
        skipPadding(buffer, valueLength);
        try {
            if (type == 1) {
                setValue(Inet4Address.getByAddress(bytes));
            } else if (type == 2) {
                setValue(Inet6Address.getByAddress(bytes));
            } else {
                throw DiameterParseException.create(buffer, "Unable to find address type", new RuntimeException());
            }
        } catch (UnknownHostException e) {
            throw DiameterParseException.create(buffer, "Unable to parse Address", e);
        }
    }

    @Override
    int convertToBytes(ByteBuffer buffer) {
        short type = (short) (value instanceof Inet4Address
                              ? 1
                              : 2);
        buffer.putShort(type);
        final byte[] bytes = value != null
                             ? value.getAddress()
                             : EMPTY;
        buffer.put(bytes);
        int length = bytes.length + 2;
        addPadding(buffer, length);
        return length;
    }

    @Override
    int convertToBytes(ByteBuf buffer) {
        short type = (short) (value instanceof Inet4Address
                              ? 1
                              : 2);
        buffer.writeShort(type);
        final byte[] bytes = value != null
                             ? value.getAddress()
                             : EMPTY;
        buffer.writeBytes(bytes);
        int length = bytes.length + 2;
        addPadding(buffer, length);
        return length;
    }
}
