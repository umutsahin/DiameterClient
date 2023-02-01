package com.optiva.diameter;

import java.nio.ByteBuffer;

public class DiameterMessageHeader {
    private int applicationId;
    private DiameterCommandCode commandCode;
    private long endToEndId;
    private long hopByHopId;
    private boolean isError;
    private boolean isProxyable;
    private boolean isRequest;
    private boolean isRetransmit;
    private int messageLength;
    private byte version = 1;

    public DiameterMessageHeader(ByteBuffer buffer) {
        final int versionAndLength = buffer.getInt();
        version = (byte) (versionAndLength >> 24);
        messageLength = versionAndLength & 0x00FFFFFF;

        final int flagsAndCommandCode = buffer.getInt();
        isRequest = (flagsAndCommandCode >>> 31) == 1;
        isProxyable = ((flagsAndCommandCode >>> 30) & 0x01) == 1;
        isError = ((flagsAndCommandCode >>> 29) & 0x01) == 1;
        isRetransmit = ((flagsAndCommandCode >>> 28) & 0x01) == 1;
        commandCode = DiameterCommandCode.getCommandCode(flagsAndCommandCode & 0x00FFFFFF);

        applicationId = buffer.getInt();
        hopByHopId = getUnsignedLong(buffer.getInt());
        endToEndId = getUnsignedLong(buffer.getInt());
    }

    private DiameterMessageHeader(){

    }

	/*
		Diameter Header from RFC
		https://tools.ietf.org/html/rfc6733#page-34

	       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |    Version    |                 Message Length                |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      | Command Flags |                  Command Code                 |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                         Application-ID                        |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                      Hop-by-Hop Identifier                    |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                      End-to-End Identifier                    |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |  AVPs ...
      +-+-+-+-+-+-+-+-+-+-+-+-+-

	 */

    public void convertToBytes(ByteBuffer buffer, int messageLength) {
        buffer.putInt((version << 24) | messageLength);
        int flags = (((((isRequest ? 1 : 0) * 2) + (isProxyable ? 1 : 0)) * 2 + (isError ? 1 : 0)) * 2 + (isRetransmit ? 1 : 0)) * 268435456;
        buffer.putInt(flags | commandCode.getCode());
        buffer.putInt(applicationId);
        buffer.putInt(toSignedInt(hopByHopId));
        buffer.putInt(toSignedInt(endToEndId));
    }

    private static long getUnsignedLong(int x) {
        return x & 0x00000000ffffffffL;
    }

    private static int toSignedInt(long x) {
        return (int) (x & 0x00000000ffffffffL);
    }

    public byte getVersion() {
        return version;
    }


    public int getMessageLength() {
        return messageLength;
    }

    public boolean isFlagR() {
        return isRequest;
    }

    public boolean isFlagP() {
        return isProxyable;
    }

    public boolean isFlagE() {
        return isError;
    }

    public boolean isFlagT() {
        return isRetransmit;
    }

    public DiameterCommandCode getCommandCode() {
        return commandCode;
    }

    public long getApplicationId() {
        return applicationId;
    }

    public long getHopByHopId() {
        return hopByHopId;
    }

    public long getEndToEndId() {
        return endToEndId;
    }

    public void setHopByHopId(long hopByHopId) {
        this.hopByHopId = hopByHopId;
    }

    public void setEndToEndId(long endToEndId) {
        this.endToEndId = endToEndId;
    }

    public DiameterMessageHeader setRequest(boolean request) {
        isRequest = request;
        return this;
    }

    @Override
    public String toString() {
        return "Header{" +
                "commandCode=" + commandCode +
                ", isRequest=" + isRequest +
                '}';
    }

    public static class Builder {
        private DiameterMessageHeader header;

        public Builder(DiameterCommandCode commandCode) {
            header = new DiameterMessageHeader();
            header.commandCode = commandCode;
        }

        public DiameterMessageHeader build() {
            return header;
        }

        public Builder setApplicationId(long applicationId) {
            header.applicationId = Long.valueOf(applicationId).intValue();
            return this;
        }

        public Builder setEndToEndId(long endToEndId) {
            header.endToEndId = Long.valueOf(endToEndId).intValue();
            return this;
        }

        public Builder setError() {
            header.isError = true;
            return this;
        }

        public Builder setHopByHopId(long hopByHopId) {
            header.hopByHopId = Long.valueOf(hopByHopId).intValue();
            return this;
        }

        public Builder setProxyable() {
            header.isProxyable = true;
            return this;
        }

        public Builder setRequest() {
            header.isRequest = true;
            return this;
        }

        public Builder setVersion(byte version) {
            header.version = version;
            return this;
        }
    }
}
