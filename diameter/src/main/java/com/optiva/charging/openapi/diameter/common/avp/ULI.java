package com.optiva.charging.openapi.diameter.common.avp;

import com.optiva.charging.openapi.diameter.common.DatatypeConverter;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public record ULI(Type type, String mcc, String mnc, String field1, String field2) {
    public enum Type {
        CGI(0),
        SAI(1),
        RAI(2),
        TAI(128),
        ECGI(129),
        TAI_AND_ECGI(130),
        GGSN_MCC_MNC(999);

        private final Integer value;

        private static final Map<Integer, Type> valueMap = new HashMap<>();

        static {
            for (Type b : Type.values()) {
                valueMap.put(b.value, b);
            }
        }

        Type(int code) {
            this.value = code;
        }

        public Integer value() {
            return value;
        }
    }

    public static final class Decoder {
        private Decoder() {
        }

        public static ULI decode(byte[] rawData) {
            ByteBuffer data = ByteBuffer.wrap(rawData);
            byte bType = data.get();
            int type = bType < 0
                       ? (bType + 0x100)
                       : bType;
            return switch (type) {
                case 0 -> decodeCGI(data);
                case 1 -> decodeSAI(data);
                case 2 -> decodeRAI(data);
                case 128 -> decodeTAI(data);
                case 129 -> decodeECGI(data);
                case 130 -> decodeTAInECGI(data);
                case 999 -> decodeGGSN(data);
                default -> throw new UnsupportedOperationException("ULI type " + type + " not supported");
            };
        }

        private static ULI decodeCGI(ByteBuffer data) {
            String[] mccMnc = decodeMccMnc(data);
            int lac = data.getShort();
            int ci = data.getShort();
            return new ULI(Type.CGI, mccMnc[0], mccMnc[1], String.valueOf(lac), String.valueOf(ci));
        }

        private static ULI decodeSAI(ByteBuffer data) {
            String[] mccMnc = decodeMccMnc(data);
            int lac = data.getShort();
            int sac = data.getShort();
            return new ULI(Type.SAI, mccMnc[0], mccMnc[1], String.valueOf(lac), String.valueOf(sac));
        }

        private static ULI decodeRAI(ByteBuffer data) {
            String[] mccMnc = decodeMccMnc(data);
            int lac = data.getShort();
            int rac = data.get();
            rac = rac < 0
                  ? rac + 0x100
                  : rac;
            return new ULI(Type.RAI, mccMnc[0], mccMnc[1], String.valueOf(lac), String.valueOf(rac));
        }

        private static ULI decodeTAI(ByteBuffer data) {
            String[] mccMnc = decodeMccMnc(data);
            String tac = decodeHexadecimal(data, 2);
            return new ULI(Type.TAI, mccMnc[0], mccMnc[1], tac, null);
        }

        private static ULI decodeECGI(ByteBuffer data) {
            String[] mccMnc = decodeMccMnc(data);
            String ecgi = decodeHexadecimal(data, 4).substring(1);
            return new ULI(Type.ECGI, mccMnc[0], mccMnc[1], ecgi, null);
        }

        private static ULI decodeTAInECGI(ByteBuffer data) {
            String[] mccMnc = decodeMccMnc(data);
            byte[] tacData = new byte[2];
            data.get(tacData);
            String tac = DatatypeConverter.printHexBinary(tacData);
            data.position(data.position() + 3);
            byte[] eciData = new byte[4];
            data.get(eciData);
            String eci = DatatypeConverter.printHexBinary(eciData).substring(1);
            return new ULI(Type.TAI_AND_ECGI, mccMnc[0], mccMnc[1], tac, eci);
        }

        public static ULI decodeGGSN(ByteBuffer data) {
            String[] mccMnc = decodeMccMnc(data);
            return new ULI(Type.GGSN_MCC_MNC, mccMnc[0], mccMnc[1], null, null);
        }

        private static String[] decodeMccMnc(ByteBuffer data) {
            StringBuilder mcc = new StringBuilder();
            StringBuilder mnc = new StringBuilder();
            byte mcc1 = data.get();
            mcc.append((byte) (mcc1 & 0x0f));
            mcc.append((byte) ((mcc1 & 0xf0) >>> 4));
            byte mccMnc = data.get();
            mcc.append((byte) (mccMnc & 0x0f));
            byte mncD3 = (byte) ((mccMnc & 0xf0) >>> 4);
            boolean twoDigit = mncD3 == 15;
            byte mnc1 = data.get();
            mnc.append((byte) (mnc1 & 0x0f));
            mnc.append((byte) ((mnc1 & 0xf0) >>> 4));
            if (!twoDigit) {
                mnc.append(mncD3);
            }
            return new String[]{mcc.toString(), mnc.toString()};
        }

        private static String decodeHexadecimal(ByteBuffer data, int size) {
            byte[] lacData = new byte[size];
            data.get(lacData);
            return DatatypeConverter.printHexBinary(lacData);
        }

        public static String decodeIPv4FromBytes(byte[] data) {
            return data[0] + "." + data[1] + "." + data[2] + "." + data[3];
        }
    }

    public static class Encoder {
        private Encoder() {
        }

        public static byte[] encode(ULI uli) {
            ByteBuffer buffer = ByteBuffer.allocate(100);
            buffer.put(uli.type().value().byteValue());
            encodeMccMnc(uli, buffer);
            switch (uli.type()) {
                case CGI -> encodeCGI(uli, buffer);
                case SAI -> encodeSAI(uli, buffer);
                default -> throw new UnsupportedOperationException("ULI type " + uli.type() + " not supported");
            }
            byte[] bytes = new byte[buffer.position()];
            buffer.get(0, bytes);
            return bytes;
        }

        private static void encodeMccMnc(ULI uli, ByteBuffer buffer) {
            char[] mcc = uli.mcc().toCharArray();
            char[] mnc = uli.mnc().toCharArray();
            buffer.put((byte) ((mcc[0] - 48) + (((mcc[1] - 48) & 0x0f) << 4)));
            buffer.put((byte) ((mnc.length == 2
                                ? 0xF0
                                : (((mcc[2] - 48) & 0x0f) << 4)) + (mcc[2] - 48)));
            buffer.put((byte) ((mnc[0] - 48) + (((mnc[1] - 48) & 0x0f) << 4)));
        }

        private static void encodeCGI(ULI uli, ByteBuffer buffer) {
            encode2ShortFields(uli, buffer);
        }

        private static void encodeSAI(ULI uli, ByteBuffer buffer) {
            encode2ShortFields(uli, buffer);
        }

        private static void encode2ShortFields(ULI uli, ByteBuffer buffer) {
            buffer.putShort(Short.parseShort(uli.field1()));
            buffer.putShort(Short.parseShort(uli.field2()));
        }
    }
}
