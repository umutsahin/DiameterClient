package com.optiva.diameter;

import java.nio.ByteBuffer;

public class DiameterAvpHeader {
    private DiameterAvpCode avpCode;
    private int avpLength;
    private boolean isMandatory;
    private boolean isProtected;
    private boolean isVendorSpecific;
    private int vendorId;
    private int avpValueLength;
    private int avpHeaderLength;

    DiameterAvpHeader(ByteBuffer buffer) {
        final int avp = buffer.getInt();
        final int flagsAndLength = buffer.getInt();
        isVendorSpecific = (flagsAndLength >>> 31) == 1;
        isMandatory = ((flagsAndLength >>> 30) & 0x01) == 1;
        isProtected = ((flagsAndLength >>> 29) & 0x01) == 1;
        avpLength = flagsAndLength & 0x00FFFFFF;

        vendorId = 0;
        avpValueLength = avpLength;
        if (isVendorSpecific) {
            vendorId = buffer.getInt();
            avpValueLength -= 4;
        }
        avpCode = DiameterAvpCodes.get(vendorId, avp);
        avpValueLength -= 8;
        avpHeaderLength = avpLength - avpValueLength;
    }

    DiameterAvpHeader(){
    }

    DiameterAvpHeader adjust() {
        avpHeaderLength = isVendorSpecific ? 12 : 8;
        avpLength = avpHeaderLength + avpValueLength;
        return this;
    }

    /*
        Diameter AVP Header from RFC
        https://tools.ietf.org/html/rfc6733#page-41

       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                           AVP Code                            |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |V M P r r r r r|                  AVP Length                   |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                        Vendor-ID (opt)                        |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |    Data ...
      +-+-+-+-+-+-+-+-+
    */

    public void convertToBytes(ByteBuffer buffer, int bytesWritten) {
        buffer.putInt(avpCode.getCode());
        int flags = ((((isVendorSpecific ? 1 : 0) * 2) + (isMandatory ? 1 : 0)) * 2 + (isProtected ? 1 : 0)) * 536870912;
        avpLength = avpHeaderLength + bytesWritten;
        buffer.putInt(flags | avpLength);
        if (isVendorSpecific) {
            buffer.putInt(vendorId);
        }
    }

    @Override
    public String toString() {
        return "AvpHeader{" +
                "avpCode=" + avpCode +
                '}';
    }

    public DiameterAvpCode getAvpCode() {
        return avpCode;
    }

    public int getAvpLength() {
        return avpLength;
    }

    public int getHeaderLength() {
        return avpHeaderLength;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public boolean isVendorSpecific() {
        return isVendorSpecific;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getAvpValueLength() {
        return avpValueLength;
    }

    void setAvpCode(DiameterAvpCode avpCode) {
        this.avpCode = avpCode;
    }

    void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    void setProtected(boolean aProtected) {
        isProtected = aProtected;
    }

    void setVendorSpecific(boolean vendorSpecific) {
        isVendorSpecific = vendorSpecific;
    }

    void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiameterAvpHeader that = (DiameterAvpHeader) o;

        if (avpLength != that.avpLength) return false;
        if (isMandatory != that.isMandatory) return false;
        if (isProtected != that.isProtected) return false;
        if (isVendorSpecific != that.isVendorSpecific) return false;
        if (vendorId != that.vendorId) return false;
        if (avpValueLength != that.avpValueLength) return false;
        if (avpHeaderLength != that.avpHeaderLength) return false;
        return avpCode != null ? avpCode.equals(that.avpCode) : that.avpCode == null;
    }

    @Override
    public int hashCode() {
        int result = avpCode != null ? avpCode.hashCode() : 0;
        result = 31 * result + avpLength;
        result = 31 * result + (isMandatory ? 1 : 0);
        result = 31 * result + (isProtected ? 1 : 0);
        result = 31 * result + (isVendorSpecific ? 1 : 0);
        result = 31 * result + vendorId;
        result = 31 * result + avpValueLength;
        result = 31 * result + avpHeaderLength;
        return result;
    }
}
