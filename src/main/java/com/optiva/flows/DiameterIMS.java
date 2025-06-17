package com.optiva.flows;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.DiameterMessageHeader;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCode;
import com.optiva.charging.openapi.diameter.common.DiameterUtil;
import com.optiva.charging.openapi.diameter.common.enumeration.CommandCode;
import io.vertx.core.buffer.Buffer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.optiva.charging.openapi.diameter.common.DiameterUtil.createGroupAvp;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.AUTH_APPLICATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CALLED_STATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_REQUEST_NUMBER;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_REQUEST_TYPE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_TIME;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.EVENT_TIMESTAMP;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.GRANTED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_CREDIT_CONTROL;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_INDICATOR;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ORIGIN_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.RATING_GROUP;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.REQUESTED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SERVICE_CONTEXT_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SESSION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID_DATA;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID_TYPE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.USED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.APPLICATION_SERVER;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.APPLICATION_SERVER_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.CALLED_PARTY_ADDRESS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.IMS_CHARGING_IDENTIFIER;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.IMS_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.NODE_FUNCTIONALITY;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.PS_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.REPORTING_REASON;
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

public class DiameterIMS extends DiameterChargingFlow {
    private static final List<Avp> STATIC_AVPS = staticAvps();

    public DiameterIMS(String msisdn, int ratingGroup, int messageCount) {
        super(msisdn, ratingGroup, messageCount);
    }

    @Override
    public void processResponse(DiameterMessage responseMessage) {
        super.processResponse(responseMessage);
        if (!error) {
            grantedUnits = DiameterUtil.getServiceAvp(responseMessage, GRANTED_SERVICE_UNIT, CC_TIME);
        }
    }

    public Buffer ccrIMessage() {
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header, dynamicAvps(1, true, 0));
    }

    public Buffer ccrURequest() {
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header, dynamicAvps(2, true, grantedUnits.getValue()));
    }

    public Buffer ccrTRequest() {
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header,
                                     dynamicAvps(3,
                                                 false,
                                                 error
                                                 ? 0
                                                 : grantedUnits.getValue()));
    }

    private List<Avp> dynamicAvps(int requestType, boolean request, int usedUnits) {
        HashMap<AvpCode, Avp> msccValue = new HashMap<>();
        msccValue.put(RATING_GROUP, RATING_GROUP.createAvp(ratingGroup));
        if (request) {
            msccValue.put(REQUESTED_SERVICE_UNIT, REQUESTED_SERVICE_UNIT.createAvp());
        }
        if (usedUnits > 0) {
            msccValue.put(USED_SERVICE_UNIT,
                          USED_SERVICE_UNIT.createAvp(Map.of(CC_TIME, CC_TIME.createAvp(usedUnits))));
            msccValue.put(REPORTING_REASON, REPORTING_REASON.createAvp(3));
        }
        return List.of(SESSION_ID.createAvp(session),
                       EVENT_TIMESTAMP.createAvp(ZonedDateTime.now()),
                       CC_REQUEST_NUMBER.createAvp(requestNumber - 1),
                       CC_REQUEST_TYPE.createAvp(requestType),
                       SUBSCRIPTION_ID.createAvp(Map.of(SUBSCRIPTION_ID_TYPE,
                                                        SUBSCRIPTION_ID_TYPE.createAvp(0),
                                                        SUBSCRIPTION_ID_DATA,
                                                        SUBSCRIPTION_ID_DATA.createAvp(msisdn))),
                       MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(msccValue));
    }

    private final Supplier<DiameterMessageHeader> headerSupplier
            = () -> new DiameterMessageHeader.Builder(CommandCode.CC).setApplicationId(4)
            .setEndToEndId(RANDOM.nextLong())
            .setHopByHopId(RANDOM.nextLong())
            .setRequest()
            .setVersion((byte) 1)
            .build();

    private final BiFunction<DiameterMessageHeader, List<Avp>, Buffer> messageFunction = (header, dynamicAvps) -> {
        ArrayList<Avp> avps = new ArrayList<>(dynamicAvps.size() + STATIC_AVPS.size());
        avps.addAll(STATIC_AVPS);
        avps.addAll(dynamicAvps);

        return writeMessageToBuffer(new DiameterMessage(header, avps));
    };

    private static List<Avp> staticAvps() {
        Avp ims = createGroupAvp(IMS_INFORMATION,
                                 NODE_FUNCTIONALITY.createAvp(0),
                                 ROLE_OF_NODE.createAvp(0),
                                 CALLED_PARTY_ADDRESS.createAvp("tel:+447408777707@optiva.com;user=phone"),
                                 createGroupAvp(SDP_MEDIA_COMPONENT, SDP_MEDIA_NAME.createAvp("audio")),
                                 createGroupAvp(APPLICATION_SERVER_INFORMATION,
                                                APPLICATION_SERVER.createAvp("OMIT_STR")),
                                 IMS_CHARGING_IDENTIFIER.createAvp("8d1f330018@msc42.truphone.com"));
        Avp ps = createGroupAvp(PS_INFORMATION, TGPP_USER_LOCATION_INFO.createAvp(parseHexBinary("0032f45200a301c8")));

        return List.of(ORIGIN_HOST.createAvp("diameterclient"),
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
