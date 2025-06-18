package com.optiva.flows;

import com.optiva.charging.openapi.diameter.avp.Avp;

import java.util.List;

import static com.optiva.charging.openapi.diameter.common.DiameterUtil.createGroupAvp;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.AUTH_APPLICATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CALLED_STATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_INPUT_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_OUTPUT_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_TOTAL_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_INDICATOR;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SERVICE_CONTEXT_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.USED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.PS_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.SERVICE_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.TGPP_USER_LOCATION_INFO;
import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

public class DiameterPS extends DiameterChargingFlow {
    private static final List<Avp> STATIC_AVPS;

    public DiameterPS(String msisdn, int ratingGroup, int messageCount) {
        super(msisdn, ratingGroup, messageCount);
    }

    @Override
    protected List<Avp> staticAvps() {
        return STATIC_AVPS;
    }

    @Override
    protected Avp usedServiceUnitsAvp(Avp grantedUnits) {
        long value = grantedUnits.getValue();
        long halfValue = value / 2;
        return createGroupAvp(USED_SERVICE_UNIT,
                              CC_TOTAL_OCTETS.createAvp(value),
                              CC_INPUT_OCTETS.createAvp(halfValue),
                              CC_OUTPUT_OCTETS.createAvp(halfValue));
    }

    static {
        Avp ps = createGroupAvp(PS_INFORMATION,
                                CALLED_STATION_ID.createAvp("iot.truphone.com"),
                                TGPP_USER_LOCATION_INFO.createAvp(parseHexBinary("0162f2102f4c6bb6")));
        STATIC_AVPS = List.of(ORIGIN_HOST.createAvp("diameterclient"),
                              ORIGIN_REALM.createAvp("optiva-test"),
                              DESTINATION_HOST.createAvp("IoT"),
                              DESTINATION_REALM.createAvp("optiva"),
                              AUTH_APPLICATION_ID.createAvp(4),
                              SERVICE_CONTEXT_ID.createAvp("32251@3gpp.org"),
                              MULTIPLE_SERVICES_INDICATOR.createAvp(1),
                              createGroupAvp(SERVICE_INFORMATION, ps));

    }
}
