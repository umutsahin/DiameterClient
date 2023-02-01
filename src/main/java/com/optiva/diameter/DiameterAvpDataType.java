package com.optiva.diameter;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public interface DiameterAvpDataType<T> {

    DiameterAvpDataType GROUPED = new DiameterGroupAvp("GROUPED");
    DiameterAvpDataType ENUMERATED = new DiameterIntegerAvp("ENUMERATED");
    DiameterAvpDataType INTEGER_32 = new DiameterIntegerAvp("INTEGER_32");
    DiameterAvpDataType INTEGER_64 = new DiameterLongAvp("INTEGER_64");
    DiameterAvpDataType OCTET_STRING = new DiameterByteArrayAvp("OCTET_STRING");
    DiameterAvpDataType ADDRESS = new DiameterByteArrayAvp("ADDRESS");
    DiameterAvpDataType UNSIGNED_32 = new DiameterIntegerAvp("UNSIGNED_32");
    DiameterAvpDataType UNSIGNED_64 = new DiameterLongAvp("UNSIGNED_64");
    DiameterAvpDataType UTF8_STRING = new DiameterStringAvp("UTF8_STRING");
    DiameterAvpDataType DIAMETER_IDENTITY = new DiameterStringAvp("DIAMETER_IDENTITY");;
    DiameterAvpDataType FLOAT_32 = new DiameterFloatAvp("FLOAT_32");
    DiameterAvpDataType FLOAT_64 = new DiameterDoubleAvp("FLOAT_64");
    DiameterAvpDataType TIME = new DiameterByteArrayAvp("TIME");

    int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder, T value);

    T readFromBytes(ByteBuffer buffer, CharsetDecoder decoder, int valueLength);

    T getDefaultValue();

    T convertValue(Object value);

    class DiameterValueCastException extends RuntimeException {
        public DiameterValueCastException(Object value) {
            super("Can not cast value:" + value);
        }
    }

    abstract class DiameterBaseAvp {
        private static final byte PADDING = (byte) 0;
        private final String name;

        static void addPadding(ByteBuffer buffer, int valueLength) {
            final int mod = valueLength % 4;
            if (mod == 0) {
                return;
            }
            for (int i = 0; i < (4 - mod); i++) {
                buffer.put(PADDING);
            }
        }

        static void skipPadding(ByteBuffer buffer, int valueLength) {
            int padding = valueLength % 4;
            if (padding > 0) {
                padding = 4 - padding;
                buffer.position(buffer.position() + padding);
            }
        }

        public DiameterBaseAvp(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    class DiameterByteArrayAvp extends DiameterBaseAvp implements DiameterAvpDataType<byte[]> {
        private static final byte[] EMPTY = new byte[0];

        private DiameterByteArrayAvp(String name) {
            super(name);
        }

        @Override
        public int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder, byte[] value) {
            final byte[] bytes = value != null ? value : EMPTY;
            buffer.put(bytes);
            addPadding(buffer, bytes.length);
            return bytes.length;
        }

        @Override
        public byte[] readFromBytes(ByteBuffer buffer, CharsetDecoder decoder, int valueLength) {
            final byte[] bytes = new byte[valueLength];
            buffer.get(bytes);
            skipPadding(buffer, valueLength);
            return bytes;
        }

        @Override
        public byte[] getDefaultValue() {
            return EMPTY;
        }

        @Override
        public byte[] convertValue(Object value) {
            if (value == null) {
                return getDefaultValue();
            }
            if (value instanceof byte[]) {
                return (byte[]) value;
            }
            throw new DiameterValueCastException(value);
        }
    }

    class DiameterIntegerAvp extends DiameterBaseAvp implements DiameterAvpDataType<Integer> {
        private static Integer ZERO = 0;

        private DiameterIntegerAvp(String name) {
            super(name);
        }

        @Override
        public int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder, Integer value) {
            buffer.putInt(value != null ? value : ZERO);
            return 4;
        }

        @Override
        public Integer readFromBytes(ByteBuffer buffer, CharsetDecoder decoder, int valueLength) {
            return buffer.getInt();
        }

        @Override
        public Integer getDefaultValue() {
            return ZERO;
        }

        @Override
        public Integer convertValue(Object value) {
            if (value == null) {
                return getDefaultValue();
            }
            if (value instanceof Integer) {
                return ((Integer) value);
            }
            if (value instanceof Byte) {
                return ((Byte) value).intValue();
            }
            if (value instanceof Short) {
                return ((Short) value).intValue();
            }
            if (value instanceof Long) {
                return ((Long) value).intValue();
            }
            if (value instanceof Double) {
                return ((Double) value).intValue();
            }
            if (value instanceof Float) {
                return ((Float) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (Exception ignored) {
                }
            }
            throw new DiameterValueCastException(value);
        }
    }

    class DiameterLongAvp extends DiameterBaseAvp implements DiameterAvpDataType<Long> {
        private static Long ZERO = 0L;

        private DiameterLongAvp(String name) {
            super(name);
        }

        @Override
        public int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder, Long value) {
            buffer.putLong(value != null ? value : ZERO);
            return 8;
        }

        @Override
        public Long readFromBytes(ByteBuffer buffer, CharsetDecoder decoder, int valueLength) {
            return buffer.getLong();
        }

        @Override
        public Long getDefaultValue() {
            return ZERO;
        }

        @Override
        public Long convertValue(Object value) {
            if (value == null) {
                return getDefaultValue();
            }
            if (value instanceof Long) {
                return ((Long) value);
            }
            if (value instanceof Byte) {
                return ((Byte) value).longValue();
            }
            if (value instanceof Short) {
                return ((Short) value).longValue();
            }
            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            }
            if (value instanceof Double) {
                return ((Double) value).longValue();
            }
            if (value instanceof Float) {
                return ((Float) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (Exception ignored) {
                }
            }
            throw new DiameterValueCastException(value);
        }
    }

    class DiameterFloatAvp extends DiameterBaseAvp implements DiameterAvpDataType<Float> {
        private static Float ZERO = 0F;

        private DiameterFloatAvp(String name) {
            super(name);
        }

        @Override
        public int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder, Float value) {
            buffer.putFloat(value != null ? value : ZERO);
            return 4;
        }

        @Override
        public Float readFromBytes(ByteBuffer buffer, CharsetDecoder decoder, int valueLength) {
            return buffer.getFloat();
        }

        @Override
        public Float getDefaultValue() {
            return ZERO;
        }

        @Override
        public Float convertValue(Object value) {
            if (value == null) {
                return getDefaultValue();
            }
            if (value instanceof Float) {
                return ((Float) value);
            }
            if (value instanceof Byte) {
                return ((Byte) value).floatValue();
            }
            if (value instanceof Short) {
                return ((Short) value).floatValue();
            }
            if (value instanceof Integer) {
                return ((Integer) value).floatValue();
            }
            if (value instanceof Double) {
                return ((Double) value).floatValue();
            }
            if (value instanceof Long) {
                return ((Long) value).floatValue();
            }
            if (value instanceof String) {
                try {
                    return Float.parseFloat((String) value);
                } catch (Exception ignored) {
                }
            }
            throw new DiameterValueCastException(value);
        }
    }

    class DiameterDoubleAvp extends DiameterBaseAvp implements DiameterAvpDataType<Double> {
        private static Double ZERO = 0D;

        private DiameterDoubleAvp(String name) {
            super(name);
        }

        @Override
        public int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder, Double value) {
            buffer.putDouble(value != null ? value : ZERO);
            return 8;
        }

        @Override
        public Double readFromBytes(ByteBuffer buffer, CharsetDecoder decoder, int valueLength) {
            return buffer.getDouble();
        }

        @Override
        public Double getDefaultValue() {
            return ZERO;
        }

        @Override
        public Double convertValue(Object value) {
            if (value == null) {
                return getDefaultValue();
            }
            if (value instanceof Double) {
                return ((Double) value);
            }
            if (value instanceof Byte) {
                return ((Byte) value).doubleValue();
            }
            if (value instanceof Short) {
                return ((Short) value).doubleValue();
            }
            if (value instanceof Integer) {
                return ((Integer) value).doubleValue();
            }
            if (value instanceof Float) {
                return ((Float) value).doubleValue();
            }
            if (value instanceof Long) {
                return ((Long) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (Exception ignored) {
                }
            }
            throw new DiameterValueCastException(value);
        }
    }

    class DiameterStringAvp extends DiameterBaseAvp implements DiameterAvpDataType<String> {
        private static final String EMPTY = "";

        private DiameterStringAvp(String name) {
            super(name);
        }

        @Override
        public int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder, String value) {
            int length = buffer.position();
            encoder.encode(CharBuffer.wrap(value != null ? value : EMPTY), buffer, false);
            length = buffer.position() - length;
            addPadding(buffer, length);
            return length;
        }

        @Override
        public String readFromBytes(ByteBuffer buffer, CharsetDecoder decoder, int valueLength) {
            final int oldLimit = buffer.limit();
            buffer.limit(buffer.position() + valueLength);
            try {
                return decoder.decode(buffer).toString();
            } catch (CharacterCodingException e) {
//                EnvironmentImpl.getEnvironment().getTracer().topicError().errorApplicationMajor(DiameterAvpDataType.class.getSimpleName(),
//                        "readFromBytes", 37, null, "Error while reading string", e, null);
                //todo
            } finally {
                buffer.limit(oldLimit);
                skipPadding(buffer, valueLength);
            }
            return null;
        }

        @Override
        public String getDefaultValue() {
            return EMPTY;
        }

        @Override
        public String convertValue(Object value) {
            if (value == null) {
                return getDefaultValue();
            }
            if (value instanceof String) {
                return (String) value;
            }
            throw new DiameterValueCastException(value);
        }
    }

    class DiameterGroupAvp extends DiameterBaseAvp implements DiameterAvpDataType<DiameterGroupedAvp> {
        private static DiameterGroupedAvp EMPTY = new DiameterGroupedAvp();

        private DiameterGroupAvp(String name) {
            super(name);
        }

        @Override
        public int convertToBytes(ByteBuffer buffer, CharsetEncoder encoder, DiameterGroupedAvp value) {
            final DiameterGroupedAvp groupedAvp = value != null ? value : EMPTY;
            final int startPosition = buffer.position();
            groupedAvp.convertToBytes(buffer, encoder);
            return buffer.position() - startPosition;
        }

        @Override
        public DiameterGroupedAvp readFromBytes(ByteBuffer buffer, CharsetDecoder decoder, int valueLength) {
            return new DiameterGroupedAvp(buffer, decoder, valueLength);
        }

        @Override
        public DiameterGroupedAvp getDefaultValue() {
            return EMPTY;
        }

        @Override
        public DiameterGroupedAvp convertValue(Object value) {
            if (value == null) {
                return getDefaultValue();
            }
            if (value instanceof DiameterGroupedAvp) {
                return (DiameterGroupedAvp) value;
            }
            throw new DiameterValueCastException(value);
        }
    }
}
