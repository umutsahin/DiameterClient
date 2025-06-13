package com.optiva.flows;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.DiameterMessageHeader;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCode;
import com.optiva.charging.openapi.diameter.common.enumeration.CommandCode;
import io.vertx.core.buffer.Buffer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.AUTH_APPLICATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CALLED_STATION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_INPUT_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_OUTPUT_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_REQUEST_NUMBER;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_REQUEST_TYPE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_TOTAL_OCTETS;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_HOST;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.DESTINATION_REALM;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.EVENT_TIMESTAMP;
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
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.PS_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.REPORTING_REASON;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.SERVICE_INFORMATION;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.TGPP_USER_LOCATION_INFO;
import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

public class DiameterFBC implements DiameterFlow {

    private static final List<Avp> STATIC_AVPS = staticAvps();
    private final String msisdn;
    private final int ratingGroup;
    private final int messageCount;
    private final String session;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private int requestNumber;

    public DiameterFBC(String msisdn, int ratingGroup, int messageCount) {
        this.msisdn = msisdn;
        this.ratingGroup = ratingGroup;
        this.messageCount = messageCount;
        this.session = UUID.randomUUID().toString();
        this.requestNumber = 1;
    }

    @Override
    public Buffer getNextMessage() { // Changed return type
        if (requestNumber == 1) {
            return ccrIMessage();
        } else if (requestNumber > 1 && requestNumber < messageCount) {
            return ccrURequest();
        } else if (requestNumber == messageCount) {
            return ccrTRequest();
        } else {
            return null;
        }
    }

    @Override
    public boolean isInitialized() {
        return requestNumber > 1;
    }

    @Override
    public DiameterFlow terminate() {
        requestNumber = messageCount;
        return this;
    }

    @Override
    public String getKey() {
        return session;
    }

    @Override
    public DiameterFlow restart() {
        return new DiameterFBC(msisdn, ratingGroup, messageCount);
    }

    public Buffer ccrIMessage() { // Changed return type
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header, dynamicAvps(1, true, 0));
    }

    public Buffer ccrURequest() { // Changed return type
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header, dynamicAvps(2, true, 1000L));
    }

    public Buffer ccrTRequest() { // Changed return type
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header, dynamicAvps(3, false, 1000L));
    }

    private List<Avp> dynamicAvps(int requestType, boolean request, long usedUnits) {
        HashMap<AvpCode, Avp> msccValue = new HashMap<>();
        msccValue.put(RATING_GROUP, RATING_GROUP.createAvp(ratingGroup));
        if (request) {
            msccValue.put(REQUESTED_SERVICE_UNIT, REQUESTED_SERVICE_UNIT.createAvp());
        }
        if (usedUnits > 0) {
            msccValue.put(USED_SERVICE_UNIT,
                          USED_SERVICE_UNIT.createAvp(Map.of(CC_TOTAL_OCTETS,
                                                             CC_TOTAL_OCTETS.createAvp(usedUnits),
                                                             CC_INPUT_OCTETS,
                                                             CC_INPUT_OCTETS.createAvp(usedUnits / 2),
                                                             CC_OUTPUT_OCTETS,
                                                             CC_OUTPUT_OCTETS.createAvp(usedUnits / 2))));
            msccValue.put(REPORTING_REASON, REPORTING_REASON.createAvp(3));
        }
        return List.of(SESSION_ID.createAvp(session),
                       EVENT_TIMESTAMP.createAvp(ZonedDateTime.now()),
                       CC_REQUEST_NUMBER.createAvp(requestNumber++ - 1),
                       CC_REQUEST_TYPE.createAvp(requestType),
                       SUBSCRIPTION_ID.createAvp(Map.of(SUBSCRIPTION_ID_TYPE,
                                                        SUBSCRIPTION_ID_TYPE.createAvp(0),
                                                        SUBSCRIPTION_ID_DATA,
                                                        SUBSCRIPTION_ID_DATA.createAvp(msisdn))),
                       MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(msccValue));
    }

    private final Supplier<DiameterMessageHeader> headerSupplier
            = () -> new DiameterMessageHeader.Builder(CommandCode.CC).setApplicationId(4)
            .setEndToEndId(random.nextLong())
            .setHopByHopId(random.nextLong())
            .setRequest()
            .setVersion((byte) 1)
            .build();

    // Changed BiFunction to return Vert.x Buffer
    private final BiFunction<DiameterMessageHeader, List<Avp>, Buffer> messageFunction = (header, dynamicAvps) -> {
        ArrayList<Avp> avps = new ArrayList<>(dynamicAvps.size() + STATIC_AVPS.size());
        avps.addAll(STATIC_AVPS);
        avps.addAll(dynamicAvps);

        return writeMessageToBuffer(new DiameterMessage(header, avps));
    };

    private static List<Avp> staticAvps() {
        return List.of(ORIGIN_HOST.createAvp("diameterclient"),
                       ORIGIN_REALM.createAvp("optiva-test"),
                       DESTINATION_HOST.createAvp("IoT"),
                       DESTINATION_REALM.createAvp("optiva"),
                       AUTH_APPLICATION_ID.createAvp(4),
                       SERVICE_CONTEXT_ID.createAvp("32251@3gpp.org"),
                       SERVICE_INFORMATION.createAvp(Map.of(PS_INFORMATION,
                                                            PS_INFORMATION.createAvp(Map.of(CALLED_STATION_ID,
                                                                                            CALLED_STATION_ID.createAvp(
                                                                                                    "iot.truphone.com"),
                                                                                            TGPP_USER_LOCATION_INFO,
                                                                                            TGPP_USER_LOCATION_INFO.createAvp(
                                                                                                    parseHexBinary(
                                                                                                            "0162f2102f4c6bb6")))))),
                       MULTIPLE_SERVICES_INDICATOR.createAvp(1));

    }
}
