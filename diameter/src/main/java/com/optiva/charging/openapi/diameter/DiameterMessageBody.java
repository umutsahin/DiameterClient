package com.optiva.charging.openapi.diameter;

import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCode;
import com.optiva.charging.openapi.diameter.exception.DiameterParseException;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiameterMessageBody {
    private Map<AvpCode, Avp> avps = new LinkedHashMap<>();

    public DiameterMessageBody() {
    }

    DiameterMessageBody(ByteBuffer buffer) {
        while (buffer.remaining() > 0) {
            try {
                final Avp avp = Avp.parseAVP(buffer);
                putOrAddAsNext(avp);
            } catch (Exception e) {
                throw DiameterParseException.create(buffer, "Problem parsing AVPs", e);
            }
        }
    }

    DiameterMessageBody(ByteBuf buffer) {
        while (buffer.readableBytes() > 0) {
            try {
                final Avp avp = Avp.parseAVP(buffer);
                putOrAddAsNext(avp);
            } catch (Exception e) {
                throw DiameterParseException.create(buffer, "Problem parsing AVPs", e);
            }
        }
    }

    public DiameterMessageBody(Collection<Avp> c) {
        for (Avp avp : c) {
            putOrAddAsNext(avp);
        }
    }

    public int convertToBytes(ByteBuffer buffer) {
        int startPosition = buffer.position();
        for (Avp avp : avps.values()) {
            avp.convertToBytes(buffer);
        }
        return buffer.position() - startPosition;
    }

    public int convertToBytes(ByteBuf buffer) {
        int startPosition = buffer.writerIndex();
        for (Avp avp : avps.values()) {
            avp.convertToBytes(buffer);
        }
        return buffer.writerIndex() - startPosition;
    }

    void putOrAddAsNext(Avp avp) {
        if (avps.containsKey(avp.getAvpCode())) {
            final Avp oldAvp = avps.get(avp.getAvpCode());
            oldAvp.setNext(avp);
        } else {
            avps.put(avp.getAvpCode(), avp);
        }
    }

    public boolean contains(AvpCode avpCode) {
        return avps.containsKey(avpCode);
    }

    public Avp getAvp(AvpCode avpCode) {
        return avps.get(avpCode);
    }

    public boolean hasAvp(AvpCode avpCode) {
        return avps.containsKey(avpCode);
    }

    public Collection<Avp> getAvpValues() {
        return avps.values();
    }

    public Map<AvpCode, Avp> getAvps() {
        return avps;
    }

    public DiameterMessageBody setAvps(Map<AvpCode, Avp> avps) {
        this.avps = avps;
        return this;
    }

    void removeFirstAvp(Avp avp) {
        final Avp removed = avps.remove(avp.getAvpCode());
        final Avp next = removed.getNext();
        if (next != null) {
            avps.put(avp.getAvpCode(), next);
        }
    }

    void removeAvp(AvpCode code) {
        avps.remove(code);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Body{\n");
        for (Avp avp : avps.values()) {
            sb.append(avp.toString().replaceAll("\n", "\n\t\t"));
            sb.append("\n");
            while (avp.getNext() != null) {
                avp = avp.getNext();
                sb.append(avp.toString().replaceAll("\n", "\n\t\t"));
                sb.append("\n");
            }
        }
        sb.append("}");

        return sb.toString();
    }
}
