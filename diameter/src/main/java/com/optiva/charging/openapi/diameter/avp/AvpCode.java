package com.optiva.charging.openapi.diameter.avp;

public final class AvpCode {
    private final Integer code;
    private final AvpDataType dataType;
    private final String name;
    private final Integer vendorId;

    public AvpCode(String name, Integer code, Integer vendorId, AvpDataType dataType) {
        this.name = name;
        this.code = code;
        this.vendorId = vendorId;
        this.dataType = dataType;
    }

    public <T> Avp createAvp(T value) {
        return new Avp.Builder(this).setMandatory().setVendor(vendorId).setValue(value).build();
    }

    public <T> Avp createAvp() {
        return new Avp.Builder(this).setMandatory().setVendor(vendorId).noValue().build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AvpCode that = (AvpCode) o;

        if (!code.equals(that.code)) {
            return false;
        }
        return vendorId.equals(that.vendorId);
    }

    public String getName() {
        return name;
    }

    public Integer getCode() {
        return this.code;
    }

    public AvpDataType getDataType() {
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
        return name + "(" + code + ")";
    }

    public String toLongString() {
        return "AvpCode{name='" + name + "', vendorId=" + vendorId + ", code=" + code + ", dataType=" + dataType + "}";
    }
}
