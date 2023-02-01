package com.optiva.diameter;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DiameterGroupedAvp {
    Map<DiameterAvpCode, DiameterAvp> avps = new LinkedHashMap<>();

    public DiameterGroupedAvp(DiameterAvp... avps) {
        for (DiameterAvp avp : avps) {
            addAvp(avp);
        }
    }

    public DiameterGroupedAvp(ByteBuffer buffer, CharsetDecoder decoder, int valueLength) {
        int readBytes = 0;
        while (valueLength > readBytes) {
            final DiameterAvp avp = DiameterAvp.parseAVP(buffer, decoder);
            addAvp(avp);
            readBytes += avp.getPaddedAvpLength();
        }
    }

    public void convertToBytes(ByteBuffer buffer, CharsetEncoder encoder) {
        for (DiameterAvp avp : avps.values()) {
            avp.convertToBytes(buffer, encoder);
        }
    }

    public DiameterAvp getAvp(DiameterAvpCode avpCode) {
        return avps.get(avpCode);
    }

    public boolean isAvpPresent(final DiameterAvpCode avpCode) {
        return avps.containsKey(avpCode);
    }

    public void addAvp(final DiameterAvp avp) {
        if (avps.containsKey(avp.getAvpCode())) {
            avps.get(avp.getAvpCode()).setNext(avp);
        } else {
            avps.put(avp.getAvpCode(), avp);
        }
    }

    public List<DiameterAvp> flatList(){
        List<DiameterAvp> flatList = new LinkedList<>();
        for (DiameterAvp value : avps.values()) {
            DiameterAvp avp = value;
            while(avp != null) {
                flatList.add(avp);
                avp = avp.getNext();
            }
        }
        return flatList;
    }

    public void addAvps(List<DiameterAvp> avps) {
        for (DiameterAvp avp : avps) {
            addAvp(avp);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GroupedAvp{avps=[");
        for (DiameterAvp avp : avps.values()) {
            sb.append(avp).append(", ");
        }
        return sb.delete(sb.length() - 2, sb.length())
                 .append("]}").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiameterGroupedAvp that = (DiameterGroupedAvp) o;

        return avps.equals(that.avps);
    }

    @Override
    public int hashCode() {
        return avps.hashCode();
    }
}
