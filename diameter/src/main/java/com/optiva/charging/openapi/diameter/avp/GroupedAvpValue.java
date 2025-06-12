package com.optiva.charging.openapi.diameter.avp;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.exception.DiameterException;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.optiva.charging.openapi.diameter.common.enumeration.ResultCode.DIAMETER_CONTRADICTING_AVPS;

public class GroupedAvpValue extends AvpValue<Map<AvpCode, Avp>> {

    @Override
    protected Map<AvpCode, Avp> getDefaultValue() {
        return new LinkedHashMap<>();
    }

    @Override
    void readFromBytes(ByteBuffer buffer, int valueLength, AvpCode avpCode) {
        int readBytes = 0;
        while (valueLength > readBytes) {
            final Avp avp = Avp.parseAVP(buffer);
            addAvp(avp);
            readBytes += avp.getPaddedAvpLength();
        }
    }

    @Override
    void readFromBytes(ByteBuf buffer, int valueLength, AvpCode avpCode) {
        int readBytes = 0;
        while (valueLength > readBytes) {
            final Avp avp = Avp.parseAVP(buffer);
            addAvp(avp);
            readBytes += avp.getPaddedAvpLength();
        }
    }

    @Override
    int convertToBytes(ByteBuffer buffer) {
        final int startPosition = buffer.position();
        for (Avp avp : value.values()) {
            avp.convertToBytes(buffer);
        }
        return buffer.position() - startPosition;
    }

    @Override
    int convertToBytes(ByteBuf buffer) {
        final int startPosition = buffer.writerIndex();
        for (Avp avp : value.values()) {
            avp.convertToBytes(buffer);
        }
        return buffer.writerIndex() - startPosition;
    }

    private void addAvp(final Avp avp) {
        if (value.containsKey(avp.getAvpCode())) {
            value.get(avp.getAvpCode()).setNext(avp);
        } else {
            value.put(avp.getAvpCode(), avp);
        }
    }

    public static void setValue(Avp groupAvp, Avp... childAvps) {
        if (groupAvp.value instanceof GroupedAvpValue) {
            Map<AvpCode, Avp> values = new LinkedHashMap<>();
            for (Avp childAvp : childAvps) {
                if (values.containsKey(childAvp.getAvpCode())) {
                    values.get(childAvp.getAvpCode()).setNext(childAvp);
                } else {
                    values.put(childAvp.getAvpCode(), childAvp);
                }
            }
            groupAvp.setValue(values);
        } else {
            throw new DiameterException("Not a GroupAvp", (DiameterMessage) null, DIAMETER_CONTRADICTING_AVPS.code());
        }
    }

    @Override
    public String toString() {
        return "GroupedAvp{" + "value=" + value.keySet()
                .stream()
                .map(key -> key + "=" + value.get(key))
                .collect(Collectors.joining(", ", "{", "}")) + "}";
    }
}
