package com.optiva.charging.openapi.diameter;

import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCode;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Collection;

public class DiameterMessage {
    private DiameterMessageHeader header;
    private DiameterMessageBody body;

    public DiameterMessage(DiameterMessageHeader header) {
        this.header = header;
        this.body = new DiameterMessageBody();
    }

    public DiameterMessage(DiameterMessageHeader header, DiameterMessageBody body) {
        this.header = header;
        this.body = body;
    }

    public DiameterMessage(DiameterMessageHeader header, Collection<Avp> avpList) {
        this.header = header;
        body = new DiameterMessageBody(avpList);
    }

    public DiameterMessage(ByteBuffer buffer) {
        setHeader(buffer);
        parseAvps(buffer);
    }

    public DiameterMessage(ByteBuf buffer) {
        setHeader(buffer);
        parseAvps(buffer);
    }

    public DiameterMessage(final byte[] rawMessage) {
        this(ByteBuffer.wrap(rawMessage));
    }

    public DiameterMessage(byte[] rawBuffer, int bufferLen) {
        this(ByteBuffer.wrap(rawBuffer, 0, bufferLen));
    }

    public DiameterMessage() {
        body = new DiameterMessageBody();
    }

    public DiameterMessageHeader getHeader() {
        return header;
    }

    public DiameterMessageBody getBody() {
        return body;
    }

    public final void setHeader(final ByteBuffer header) {
        this.header = new DiameterMessageHeader(header);
    }

    public final void setHeader(final ByteBuf header) {
        this.header = new DiameterMessageHeader(header);
    }

    public void removeFirstAvp(Avp avp) {
        body.removeFirstAvp(avp);
    }

    public DiameterMessage addAvp(Avp avp) {
        body.putOrAddAsNext(avp);
        return this;
    }

    public Avp getAvp(AvpCode avpCode) {
        return body.getAvp(avpCode);
    }

    public <T> T getAvpValue(AvpCode avpCode, T defaultValue) {
        if (body.hasAvp(avpCode)) {
            return body.getAvp(avpCode).getValue();
        }
        return defaultValue;
    }

    public <T> T getAvpValue(AvpCode avpCode) {
        return getAvpValue(avpCode, null);
    }

    public Collection<Avp> getAvps() {
        return body.getAvpValues();
    }

    public boolean isAvpPresent(final AvpCode avpCode) {
        return body.contains(avpCode);
    }

    public <T> void replaceFirstAvp(AvpCode avpCode, T value) {
        final Avp avp = body.getAvp(avpCode);
        if (avp != null) {
            avp.setValue(value);
        } else {
            addAvp(avpCode.createAvp(value));
        }
    }

    public void removeAvp(AvpCode code) {
        body.removeAvp(code);
    }

    public void replaceAvp(Avp avp) {
        body.removeAvp(avp.getAvpCode());
        body.putOrAddAsNext(avp);
    }

    public byte[] convertToBytes(ByteBuffer buffer) {
        convertToByteBuffer(buffer);
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public byte[] convertToBytes(ByteBuf buffer) {
        convertToByteBuf(buffer);
        final byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    public void convertToByteBuffer(ByteBuffer buffer) {
        int initialPosition = buffer.position();
        buffer.position(initialPosition + 20); //skip header
        int messageLength = body.convertToBytes(buffer) + 20; // 20 from header
        buffer.position(initialPosition); // rewind
        header.convertToBytes(buffer, messageLength);
        buffer.position(initialPosition);
        buffer.limit(initialPosition + messageLength);
    }

    public void convertToByteBuf(ByteBuf buffer) {
        int initialIndex = buffer.writerIndex();
        buffer.writerIndex(initialIndex + 20); // Start with AVPs
        int messageLength = body.convertToBytes(buffer) + 20; // 20 from header
        buffer.writerIndex(initialIndex); // rewind
        header.convertToBytes(buffer, messageLength);
        buffer.writerIndex(initialIndex + messageLength);
    }

    private void parseAvps(final ByteBuffer avpBuffer) {
        body = new DiameterMessageBody(avpBuffer);
    }

    private void parseAvps(final ByteBuf avpBuffer) {
        body = new DiameterMessageBody(avpBuffer);
    }

    @Override
    public String toString() {
        return "DiameterMessage{" + "\n\t" + header.toString().replaceAll("\n", "\n\t") + "\n\t" + body.toString()
                .replaceAll("\n", "\n\t") + "\n}";
    }
}
