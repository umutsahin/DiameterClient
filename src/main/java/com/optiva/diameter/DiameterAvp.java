package com.optiva.diameter;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Objects;

public class DiameterAvp {
    private DiameterAvpHeader header;
    private Object value;
    private DiameterAvp next = null;

    protected DiameterAvp(DiameterAvpHeader header, Object value) {
        this.header = header;
        this.value = value;
    }

    public static DiameterAvp parseAVP(ByteBuffer buffer, CharsetDecoder decoder) {
        DiameterAvpHeader header = new DiameterAvpHeader(buffer);

        final Object object = header.getAvpCode().getDataType().readFromBytes(buffer, decoder, header.getAvpValueLength());

        return new DiameterAvp(header, object);
    }

    public void convertToBytes(ByteBuffer buffer, CharsetEncoder encoder) {
        final int startPosition = buffer.position();
        buffer.position(startPosition + header.getHeaderLength());
        final int bytesWritten = header.getAvpCode().getDataType().convertToBytes(buffer, encoder, value);
        final int endPosition = buffer.position();
        buffer.position(startPosition);
        header.convertToBytes(buffer, bytesWritten);
        buffer.position(endPosition);
        if (next != null) {
            next.convertToBytes(buffer, encoder);
        }
    }

    public DiameterAvpCode getAvpCode() {
        return header.getAvpCode();
    }

    public DiameterAvpHeader getHeader() {
        return header;
    }

    public int getPaddedAvpLength() {
        final int mod = header.getAvpLength() % 4;
        if (mod == 0) {
            return header.getAvpLength();
        } else {
            return header.getAvpLength() + (4 - mod);
        }
    }

    public DiameterGroupedAvp getDataAsGrouped() {
        return getValue();
    }

    public Integer getDataAsInteger32() {
        return getValue();
    }

    public Integer getDataAsUnsigned32() {
        return getValue();
    }

    public Long getDataAsInteger64() {
        return getValue();
    }

    public byte[] getDataAsOctetString() {
        return getValue();
    }

    public String getDataAsUTF8String() {
        return getValue();
    }

    public Float getDataAsFloat32() {
        return getValue();
    }

    public Double getDataAsFloat64() {
        return getValue();
    }

    private <T> T getValue() {
        return (T) value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public DiameterAvp getNext() {
        return next;
    }

    public void setNext(DiameterAvp avp) {
        if (next == null) {
            next = avp;
        } else {
            next.setNext(avp);
        }
    }

    public boolean isEmpty() {
        return value == null;
    }

    public boolean isMandatory() {
        return header.isMandatory();
    }

    public boolean isProtected() {
        return header.isProtected();
    }

    @Override
    public String toString() {
        return "Avp{\n" +
                "header=" + header +
                "\nvalue=" + value +
                "\n}";
    }

    public static class Builder {
        private DiameterAvpHeader header;
        private Object value;

        public Builder(DiameterAvpCode avpCode) {
            header = new DiameterAvpHeader();
            header.setAvpCode(avpCode);
        }

        public DiameterAvp build() {
            return new DiameterAvp(header.adjust(), value);
        }

        public Builder setMandatory() {
            header.setMandatory(true);
            return this;
        }

        public Builder setValue(Object value) {
            if (value == null) {
                throw new NullPointerException("Value for avp " + header + " is null");
            }
            if (value instanceof DiameterAvp) {
                throw new IllegalArgumentException("Value for avp " + header + " can not be another avp");
            }
            this.value = header.getAvpCode().getDataType().convertValue(value);
            return this;
        }

        public Builder setProtected() {
            header.setProtected(true);
            return this;
        }

        public Builder setVendor(int vendorId) {
            header.setVendorSpecific(true);
            header.setVendorId(vendorId);
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiameterAvp that = (DiameterAvp) o;

        if (!header.equals(that.header)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = header.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
