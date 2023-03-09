package com.optiva.diameter;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiameterMessageBody {
    private final Map<DiameterAvpCode, DiameterAvp> avps = new LinkedHashMap<>();

    DiameterMessageBody(ByteBuffer avpBuffer, CharsetDecoder decoder) {
        while (avpBuffer.remaining() > 0) {
            try {
                final DiameterAvp avp = DiameterAvp.parseAVP(avpBuffer, decoder);
                putOrAddAsNext(avp);
            } catch (Exception e) {
                throw DiameterParseException.create(avpBuffer, "Problem parsing AVPs", e);
            }
        }
    }

    public DiameterMessageBody(Collection<DiameterAvp> c) {
        for (DiameterAvp avp : c) {
            putOrAddAsNext(avp);
        }
    }

    public int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder) {
        int startPosition = buffer.position();
        for (DiameterAvp avp : avps.values()) {
            avp.convertToBytes(buffer, encoder);
        }
        return buffer.position() - startPosition;
    }

    void putOrAddAsNext(DiameterAvp avp) {
        if (avps.containsKey(avp.getAvpCode())) {
            final DiameterAvp oldAvp = avps.get(avp.getAvpCode());
            oldAvp.setNext(avp);
        } else {
            avps.put(avp.getAvpCode(), avp);
        }
    }

    public boolean contains(DiameterAvpCode avpCode) {
        return avps.containsKey(avpCode);
    }

    public DiameterAvp getAvp(DiameterAvpCode avpCode) {
        return avps.get(avpCode);
    }

    public Collection<DiameterAvp> getAvps(){
        return avps.values();
    }

    void removeAvp(DiameterAvp avp) {
        final DiameterAvp removed = avps.remove(avp.getAvpCode());
        final DiameterAvp next = removed.getNext();
        if (next != null) {
            avps.put(avp.getAvpCode(), next);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Body{\n");
        for (DiameterAvp avp : avps.values()) {
            sb.append(avp.toString().replaceAll("\n", "\n\t\t"));
            sb.append("\n");
            while(avp.getNext() != null) {
                avp = avp.getNext();
                sb.append(avp.toString().replaceAll("\n", "\n\t\t"));
                sb.append("\n");
            }
        }
        sb.append("}");

        return sb.toString();
    }
}
