package com.optiva.diameter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Collection;


public class DiameterMessage {
    private DiameterMessageBody body;
    private DiameterMessageHeader header;


    public DiameterMessage(DiameterMessageHeader header, Collection<DiameterAvp> avpList) {
        this.header = header;
        body = new DiameterMessageBody(avpList);
    }

    public DiameterMessage(ByteBuffer buffer,CharsetDecoder decoder) throws IOException {
        setHeader(buffer);
        parseAvps(buffer, decoder);
    }

    public DiameterMessage(final byte[] rawMessage, CharsetDecoder decoder) throws IOException {
        this(ByteBuffer.wrap(rawMessage), decoder);
    }

    public DiameterMessage(byte[] rawBuffer, int bufferLen, CharsetDecoder decoder) throws IOException {
        this(ByteBuffer.wrap(rawBuffer, 0, bufferLen), decoder);
    }

    public DiameterMessage() {
    }

    public void removeAvp(DiameterAvp avp) {
        body.removeAvp(avp);
    }

    public DiameterMessage addAvp(DiameterAvp avp) {
        body.putOrAddAsNext(avp);
        return this;
    }

    public DiameterAvp getAvp(DiameterAvpCode avpCode) {
        return body.contains(avpCode) ? body.getAvp(avpCode) : DiameterEmptyAvp.INSTANCE;
    }

    public Collection<DiameterAvp> getAvps(){
        return body.getAvps();
    }

    public DiameterMessageHeader getHeader() {
        return header;
    }

    public final void setHeader(final ByteBuffer header) throws IOException {
        this.header = new DiameterMessageHeader(header);
    }

    public byte[] convertToBytes(ByteBuffer buffer, CharsetEncoder encoder) {
        convertToByteBuffer(buffer, encoder);
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public void convertToByteBuffer(ByteBuffer buffer, CharsetEncoder encoder) {
        buffer.clear();
        buffer.position(20); //skip header
        int messageLength = body.convertToBytes(buffer, encoder) + 20; // 20 from header
        buffer.position(0); // rewind
        header.convertToBytes(buffer, messageLength);
        buffer.position(0);
        buffer.limit(messageLength);
    }

    public boolean isAvpPresent(final DiameterAvpCode avpCode) {
        return body.contains(avpCode);
    }

    public void replaceFirstAvp(DiameterAvpCode avpCode, Object value) {
        final DiameterAvp avp = body.getAvp(avpCode);
        if (avp != null) {
            avp.setValue(value);
        } else {
            addAvp(avpCode.createManAvp(value));
        }
    }

    public final void parseAvps(final ByteBuffer avpBuffer, CharsetDecoder decoder) {
        body = new DiameterMessageBody(avpBuffer, decoder);
    }

    @Override
    public String toString() {
        return "DiameterMessage{" +
               "\n\t" +
               header.toString().replaceAll("\n", "\n\t") +
               "\n\t" +
               body.toString().replaceAll("\n", "\n\t") +
               "\n}";
    }
}
