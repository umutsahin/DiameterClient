package com.optiva.charging.openapi.diameter.avp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Objects;

public class Avp {
    private AvpHeader header;
    AvpValue<?> value;
    private Avp next = null;

    private Avp(AvpHeader header, AvpValue<?> value) {
        this.header = header;
        this.value = value;
    }

    protected Avp() {
    }

    public static Avp parseAVP(ByteBuffer buffer) {
        AvpHeader header = new AvpHeader(buffer);

        final AvpValue<?> value = header.getAvpCode().getDataType().getValueInstance();
        value.readFromBytes(buffer, header.getAvpValueLength(), header.getAvpCode());

        return new Avp(header, value);
    }

    public static Avp parseAVP(ByteBuf buffer) {
        AvpHeader header = new AvpHeader(buffer);

        final AvpValue<?> value = header.getAvpCode().getDataType().getValueInstance();
        value.readFromBytes(buffer, header.getAvpValueLength(), header.getAvpCode());

        return new Avp(header, value);
    }

    public void convertToBytes(ByteBuffer buffer) {
        final int startPosition = buffer.position();
        buffer.position(startPosition + header.getHeaderLength());
        final int bytesWritten = value.convertToBytes(buffer);
        final int endPosition = buffer.position();
        buffer.position(startPosition);
        header.convertToBytes(buffer, bytesWritten);
        buffer.position(endPosition);
        if (next != null) {
            next.convertToBytes(buffer);
        }
    }

    public void convertToBytes(ByteBuf buffer) {
        final int startPosition = buffer.writerIndex();
        buffer.writerIndex(startPosition + header.getHeaderLength());
        final int bytesWritten = value.convertToBytes(buffer);
        final int endPosition = buffer.writerIndex();
        buffer.writerIndex(startPosition);
        header.convertToBytes(buffer, bytesWritten);
        buffer.writerIndex(endPosition);
        if (next != null) {
            next.convertToBytes(buffer);
        }
    }

    public AvpCode getAvpCode() {
        return header.getAvpCode();
    }

    public int getPaddedAvpLength() {
        final int mod = header.getAvpLength() % 4;
        if (mod == 0) {
            return header.getAvpLength();
        } else {
            return header.getAvpLength() + (4 - mod);
        }
    }

    public <T> T getValue() {
        return (T) value.getValue();
    }

    public <T> void setValue(T value) {
        AvpDataType dataType = header.getAvpCode().getDataType();
        if (dataType.isNotCompatibleValueClass(value)) {
            throw new IllegalArgumentException("Expected value type for avp(%s) is \"%s\" offered value type is \"%s\"".formatted(
                    header.getAvpCode(),
                    dataType.getValueClassName(),
                    value.getClass().getSimpleName()));
        }
        ((AvpValue<T>) this.value).setValue(value);
    }

    public Avp getNext() {
        return next;
    }

    public void setNext(Avp avp) {
        if (next == null) {
            next = avp;
        } else {
            next.setNext(avp);
        }
    }

    Avp setHeader(AvpHeader header) {
        this.header = header;
        return this;
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
        return "Avp{\n" + "header=" + header + "\nvalue=" + value + "\n}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Avp that = (Avp) o;

        if (!header.equals(that.header)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = header.hashCode();
        result = 31 * result + (value != null
                                ? value.hashCode()
                                : 0);
        return result;
    }

    public static class Builder<T> {
        private AvpHeader header;
        private AvpValue<T> value;

        public Builder(AvpCode avpCode) {
            header = new AvpHeader();
            header.setAvpCode(avpCode);
        }

        public Avp build() {
            return new Avp(header.adjust(), value);
        }

        public Builder<T> setMandatory() {
            header.setMandatory(true);
            return this;
        }

        public Builder<T> setValue(T value) {
            if (value == null) {
                throw new NullPointerException("Value for avp " + header + " is null");
            }
            AvpDataType dataType = header.getAvpCode().getDataType();
            if (dataType.isNotCompatibleValueClass(value)) {
                throw new IllegalArgumentException(
                        "Expected value type for avp(%s) is \"%s\" offered value type is \"%s\"".formatted(header.getAvpCode(),
                                                                                                           dataType.getValueClassName(),
                                                                                                           value.getClass()
                                                                                                                   .getSimpleName()));
            }
            AvpValue<T> valueInstance = dataType.getValueInstance();
            this.value = valueInstance.setValue(value);
            return this;
        }

        public Builder<T> noValue() {
            this.value = header.getAvpCode().getDataType().getValueInstance();
            return this;
        }

        public Builder<T> setProtected() {
            header.setProtected(true);
            return this;
        }

        public Builder<T> setVendor(int vendorId) {
            header.setVendorId(vendorId);
            if (vendorId != 0) {
                header.setVendorSpecific(true);
            }
            return this;
        }
    }
}
