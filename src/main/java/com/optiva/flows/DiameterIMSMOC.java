package com.optiva.flows;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.common.DiameterUtil;

import java.util.List;
import java.util.Map;

import static com.optiva.charging.openapi.diameter.common.DiameterUtil.createGroupAvp;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.AUTH_APPLICATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CALLED_STATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_TIME;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.GRANTED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_INDICATOR;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SERVICE_CONTEXT_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.USED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.APPLICATION_SERVER;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.APPLICATION_SERVER_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.CALLED_PARTY_ADDRESS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.IMS_CHARGING_IDENTIFIER;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.IMS_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.NODE_FUNCTIONALITY;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.PS_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.ROLE_OF_NODE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.SDP_MEDIA_COMPONENT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.SDP_MEDIA_NAME;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.SERVICE_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.TGPP_CHARGING_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.TGPP_GGSN_ADDRESS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.TGPP_SGSN_ADDRESS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.TGPP_SGSN_MCC_MNC;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.TGPP_USER_LOCATION_INFO;
import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

public class DiameterIMSMOC extends DiameterChargingFlow {
    private static final List<Avp> STATIC_AVPS;

    public DiameterIMSMOC(String msisdn, int ratingGroup, int messageCount) {
        super(msisdn, ratingGroup, messageCount);
    }

    @Override
    public void processResponse(DiameterMessage responseMessage) {
        super.processResponse(responseMessage);
        if (!error) {
            grantedUnits = DiameterUtil.getServiceAvp(responseMessage, GRANTED_SERVICE_UNIT, CC_TIME);
        }
    }

    @Override
    protected List<Avp> staticAvps() {
        return STATIC_AVPS;
    }

    @Override
    protected Avp usedServiceUnitsAvp(Avp grantedUnits) {
        return USED_SERVICE_UNIT.createAvp(Map.of(CC_TIME, grantedUnits));
    }

    static {
        Avp ims = createGroupAvp(IMS_INFORMATION,
                                 NODE_FUNCTIONALITY.createAvp(0),
                                 ROLE_OF_NODE.createAvp(0),
                                 CALLED_PARTY_ADDRESS.createAvp("tel:+447408777707@optiva.com;user=phone"),
                                 createGroupAvp(SDP_MEDIA_COMPONENT, SDP_MEDIA_NAME.createAvp("audio")),
                                 createGroupAvp(APPLICATION_SERVER_INFORMATION,
                                                APPLICATION_SERVER.createAvp("OMIT_STR")),
                                 IMS_CHARGING_IDENTIFIER.createAvp("8d1f330018@msc42.truphone.com"));
        Avp ps = createGroupAvp(PS_INFORMATION, TGPP_USER_LOCATION_INFO.createAvp(parseHexBinary("0032f45200a301c8")));

        STATIC_AVPS = List.of(ORIGIN_HOST.createAvp("diameterclient"),
                              ORIGIN_REALM.createAvp("optiva-test"),
                              DESTINATION_HOST.createAvp("IoT"),
                              DESTINATION_REALM.createAvp("optiva"),
                              AUTH_APPLICATION_ID.createAvp(4),
                              SERVICE_CONTEXT_ID.createAvp("32260@3gpp.org"),
                              CALLED_STATION_ID.createAvp("iot.truphone.com"),
                              TGPP_SGSN_ADDRESS.createAvp(parseHexBinary("d5e23721")),
                              TGPP_CHARGING_ID.createAvp(parseHexBinary("2cad6696")),
                              TGPP_GGSN_ADDRESS.createAvp(parseHexBinary("d5e237fd")),
                              TGPP_SGSN_MCC_MNC.createAvp("28401"),
                              MULTIPLE_SERVICES_INDICATOR.createAvp(1),
                              createGroupAvp(SERVICE_INFORMATION, ims, ps));

    }
}
