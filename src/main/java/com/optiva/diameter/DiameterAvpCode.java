package com.optiva.diameter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public final class DiameterAvpCode {
    private final Integer code;
    private final DiameterAvpDataType dataType;
    private final String name;
    private final Integer vendorId;

    DiameterAvpCode(String name,
                    Integer code,
                    Integer vendorId,
                    DiameterAvpDataType dataType) {
        this.name = name;
        this.code = code;
        this.vendorId = vendorId;
        this.dataType = dataType;
    }

    public DiameterAvp create3gppManAvp(Object value) {
        return createVendorManAvp(DiameterAvpCodes.VendorId.TGPP, value);
    }

    public DiameterAvp createManAvp(Object value) {
        return new DiameterAvp.Builder(this)
                .setMandatory()
                .setValue(value)
                .build();
    }

    public DiameterAvp createVendorManAvp(int vendorId, Object value) {
        return new DiameterAvp.Builder(this)
                .setMandatory()
                .setVendor(vendorId)
                .setValue(value)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiameterAvpCode that = (DiameterAvpCode) o;

        if (!code.equals(that.code)) return false;
        return vendorId.equals(that.vendorId);
    }

    public Integer getCode() {
        return this.code;
    }

    public DiameterAvpDataType getDataType() {
        return this.dataType;
    }

    public Integer getVendorId() {
        return vendorId;
    }

    @Override
    public int hashCode() {
        int result = code.hashCode();
        result = 31 * result + vendorId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AvpCode{" + name + ", " + code + ", " + dataType + "}";
    }
}
