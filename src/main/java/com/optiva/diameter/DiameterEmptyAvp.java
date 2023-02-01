package com.optiva.diameter;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;

public class DiameterEmptyAvp extends DiameterAvp {

    public static final DiameterEmptyAvp INSTANCE = new DiameterEmptyAvp();
    private static final DiameterGroupedAvp DIAMETER_GROUPED_AVP = new DiameterGroupedAvp();

    private DiameterEmptyAvp() {
        super(null, null);
    }

    @Override
    public void convertToBytes(ByteBuffer buffer, CharsetEncoder encoder) {
    }

    @Override
    public DiameterAvpCode getAvpCode() {
        return super.getAvpCode();
    }

    @Override
    public Float getDataAsFloat32() {
        return 0F;
    }

    @Override
    public Double getDataAsFloat64() {
        return 0D;
    }

    @Override
    public DiameterGroupedAvp getDataAsGrouped() {
        return DIAMETER_GROUPED_AVP;
    }

    @Override
    public Integer getDataAsInteger32() {
        return 0;
    }

    @Override
    public Long getDataAsInteger64() {
        return 0L;
    }

    @Override
    public Integer getDataAsUnsigned32() {
        return 0;
    }
}
