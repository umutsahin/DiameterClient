package com.optiva.charging.openapi.diameter;

import com.optiva.charging.openapi.diameter.common.enumeration.CommandCode;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public class DiameterMessageHeader {
    private int applicationId;
    private CommandCode commandCode;
    private long endToEndId;
    private long hopByHopId;
    private boolean error; //
    private boolean proxyable;
    private boolean request;
    private boolean retransmit; //
    private int messageLength;
    private byte version = 1;

    /**
     * Diameter Header from RFC
     * https://tools.ietf.org/html/rfc6733#page-34
     * <p>
     * 0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |    Version    |                 Message Length                |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | Command Flags |                  Command Code                 |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         Application-ID                        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                      Hop-by-Hop Identifier                    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                      End-to-End Identifier                    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  AVPs ...
     * +-+-+-+-+-+-+-+-+-+-+-+-+-
     *
     * @param buffer
     */
    public DiameterMessageHeader(ByteBuffer buffer) {
        int position = buffer.position();
        final int versionAndLength = buffer.getInt();
        version = (byte) (versionAndLength >> 24);
        messageLength = versionAndLength & 0x00FFFFFF;

        final int flagsAndCommandCode = buffer.getInt();
        request = (flagsAndCommandCode >>> 31) == 1;
        proxyable = ((flagsAndCommandCode >>> 30) & 0x01) == 1;
        error = ((flagsAndCommandCode >>> 29) & 0x01) == 1;
        retransmit = ((flagsAndCommandCode >>> 28) & 0x01) == 1;
        commandCode = CommandCode.getCommandCode(flagsAndCommandCode & 0x00FFFFFF);

        applicationId = buffer.getInt();
        hopByHopId = getUnsignedLong(buffer.getInt());
        endToEndId = getUnsignedLong(buffer.getInt());
        buffer.limit(position + messageLength);
    }

    public DiameterMessageHeader(ByteBuf buffer) {
        int position = buffer.readerIndex();
        final int versionAndLength = buffer.readInt();
        version = (byte) (versionAndLength >> 24);
        messageLength = versionAndLength & 0x00FFFFFF;

        final int flagsAndCommandCode = buffer.readInt();
        request = (flagsAndCommandCode >>> 31) == 1;
        proxyable = ((flagsAndCommandCode >>> 30) & 0x01) == 1;
        error = ((flagsAndCommandCode >>> 29) & 0x01) == 1;
        retransmit = ((flagsAndCommandCode >>> 28) & 0x01) == 1;
        commandCode = CommandCode.getCommandCode(flagsAndCommandCode & 0x00FFFFFF);

        applicationId = buffer.readInt();
        hopByHopId = getUnsignedLong(buffer.readInt());
        endToEndId = getUnsignedLong(buffer.readInt());
        buffer.writerIndex(position + messageLength);
    }

    public DiameterMessageHeader() {

    }

    public void convertToBytes(ByteBuffer buffer, int messageLength) {
        buffer.putInt((version << 24) | messageLength);
        int flags = (((((request
                         ? 1
                         : 0) * 2) + (proxyable
                                      ? 1
                                      : 0)) * 2 + (error
                                                   ? 1
                                                   : 0)) * 2 + (retransmit
                                                                ? 1
                                                                : 0)) * 268435456;
        buffer.putInt(flags | commandCode.getCode());
        buffer.putInt(applicationId);
        buffer.putInt(toSignedInt(hopByHopId));
        buffer.putInt(toSignedInt(endToEndId));
    }

    public void convertToBytes(ByteBuf buffer, int messageLength) {
        buffer.writeInt((version << 24) | messageLength);
        int flags = (((((request
                         ? 1
                         : 0) * 2) + (proxyable
                                      ? 1
                                      : 0)) * 2 + (error
                                                   ? 1
                                                   : 0)) * 2 + (retransmit
                                                                ? 1
                                                                : 0)) * 268435456;
        buffer.writeInt(flags | commandCode.getCode());
        buffer.writeInt(applicationId);
        buffer.writeInt(toSignedInt(hopByHopId));
        buffer.writeInt(toSignedInt(endToEndId));
    }

    private static long getUnsignedLong(int x) {
        return x & 0x00000000ffffffffL;
    }

    private static int toSignedInt(long x) {
        return (int) (x & 0x00000000ffffffffL);
    }

    public int getApplicationId() {
        return applicationId;
    }

    public DiameterMessageHeader setApplicationId(int applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public CommandCode getCommandCode() {
        return commandCode;
    }

    public DiameterMessageHeader setCommandCode(CommandCode commandCode) {
        this.commandCode = commandCode;
        return this;
    }

    public long getEndToEndId() {
        return endToEndId;
    }

    public DiameterMessageHeader setEndToEndId(long endToEndId) {
        this.endToEndId = endToEndId;
        return this;
    }

    public long getHopByHopId() {
        return hopByHopId;
    }

    public DiameterMessageHeader setHopByHopId(long hopByHopId) {
        this.hopByHopId = hopByHopId;
        return this;
    }

    public boolean isError() {
        return error;
    }

    public DiameterMessageHeader setError(boolean error) {
        this.error = error;
        return this;
    }

    public boolean isProxyable() {
        return proxyable;
    }

    public DiameterMessageHeader setProxyable(boolean proxyable) {
        this.proxyable = proxyable;
        return this;
    }

    public boolean isRequest() {
        return request;
    }

    public DiameterMessageHeader setRequest(boolean request) {
        this.request = request;
        return this;
    }

    public boolean isRetransmit() {
        return retransmit;
    }

    public DiameterMessageHeader setRetransmit(boolean retransmit) {
        this.retransmit = retransmit;
        return this;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public DiameterMessageHeader setMessageLength(int messageLength) {
        this.messageLength = messageLength;
        return this;
    }

    public byte getVersion() {
        return version;
    }

    public DiameterMessageHeader setVersion(byte version) {
        this.version = version;
        return this;
    }

    public DiameterMessageHeader clone() {
        DiameterMessageHeader header = new DiameterMessageHeader();
        header.applicationId = this.applicationId;
        header.commandCode = this.commandCode;
        header.endToEndId = this.endToEndId;
        header.hopByHopId = this.hopByHopId;
        header.error = this.error;
        header.proxyable = this.proxyable;
        header.request = this.request;
        header.retransmit = this.retransmit;
        header.messageLength = 0;
        return header;
    }

    @Override
    public String toString() {
        return "Header{" + "commandCode=" + commandCode + ", isRequest=" + request + '}';
    }

    public static class Builder {
        private DiameterMessageHeader header;

        public Builder(CommandCode commandCode) {
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
            header.error = true;
            return this;
        }

        public Builder setHopByHopId(long hopByHopId) {
            header.hopByHopId = Long.valueOf(hopByHopId).intValue();
            return this;
        }

        public Builder setProxyable() {
            header.proxyable = true;
            return this;
        }

        public Builder setRequest() {
            header.request = true;
            return this;
        }

        public Builder setVersion(byte version) {
            header.version = version;
            return this;
        }
    }
}
