package com.optiva.flows;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.DiameterMessageHeader;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.common.enumeration.CommandCode;
import io.vertx.core.buffer.Buffer; // Changed import
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;

import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.HOST_IP_ADDRESS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.PRODUCT_NAME;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.VENDOR_ID;

public class DiameterCE implements DiameterFlow {
    private static final List<Avp> AVPS;

    static {
        try {
            AVPS = List.of(ORIGIN_REALM.createAvp("siemens.de"),
                           ORIGIN_HOST.createAvp("bln1.siemens.de"),
                           HOST_IP_ADDRESS.createAvp(Inet4Address.getLocalHost()),
                           PRODUCT_NAME.createAvp("test"),
                           VENDOR_ID.createAvp(1));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Buffer getNextMessage() { // Changed return type
        DiameterMessageHeader header = new DiameterMessageHeader.Builder(CommandCode.CE).setApplicationId(0)
                .setRequest()
                .setHopByHopId(RANDOM.nextLong())
                .setEndToEndId(RANDOM.nextLong())
                .build();

        return writeMessageToBuffer(new DiameterMessage(header, AVPS));
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public DiameterFlow terminate() {
        return this;
    }

    @Override
    public String getKey() {
        return "";
    }

    @Override
    public DiameterFlow restart() {
        return new DiameterCE();
    }
}
