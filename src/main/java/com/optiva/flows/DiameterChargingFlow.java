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
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.optiva.charging.openapi.diameter.common.DiameterUtil.createGroupAvp;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_REQUEST_NUMBER;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.CC_REQUEST_TYPE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.EVENT_TIMESTAMP;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_CREDIT_CONTROL;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.RATING_GROUP;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.REQUESTED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SESSION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID_DATA;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.SUBSCRIPTION_ID_TYPE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.USED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.TGPP.REPORTING_REASON;

public abstract class DiameterChargingFlow extends DiameterFlow {
    protected final int ratingGroup;
    protected final int messageCount;
    protected final String session;
    protected int requestNumber;
    protected static final Avp emptyRequestAvp = REQUESTED_SERVICE_UNIT.createAvp();
    protected static final Avp reportingReasonAvp = REPORTING_REASON.createAvp(3);
    protected static final Supplier<String> msisdn = () -> "447400000" + String.format("%04d", RANDOM.nextInt(1000));
    protected final Avp ratingGroupAvp;
    protected final Avp sessionIdAvp;
    protected final Avp subscriptionIdAvp;
    protected Avp grantedUnits;

    public DiameterChargingFlow(int ratingGroup, int messageCount) {
        this.ratingGroup = ratingGroup;
        this.messageCount = messageCount;
        this.session = UUID.randomUUID().toString();
        this.requestNumber = 1;
        ratingGroupAvp = RATING_GROUP.createAvp(ratingGroup);
        sessionIdAvp = SESSION_ID.createAvp(session);
        subscriptionIdAvp = createGroupAvp(SUBSCRIPTION_ID,
                                           SUBSCRIPTION_ID_TYPE.createAvp(0),
                                           SUBSCRIPTION_ID_DATA.createAvp(msisdn.get()));
    }

    @Override
    public Buffer getNextMessage() {
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
    public boolean isInProgress() {
        return requestNumber <= messageCount;
    }

    @Override
    public void iterateFlow() {
        requestNumber++;
    }

    @Override
    public void terminateFlow() {
        error = true;
        requestNumber = messageCount;
    }

    @Override
    public String getKey() {
        return session;
    }

    private Buffer ccrIMessage() {
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header, dynamicAvps(1, true, null));
    }

    private Buffer ccrURequest() {
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header, dynamicAvps(2, true, grantedUnits));
    }

    private Buffer ccrTRequest() {
        DiameterMessageHeader header = headerSupplier.get();
        return messageFunction.apply(header,
                                     dynamicAvps(3,
                                                 false,
                                                 error
                                                 ? null
                                                 : grantedUnits));
    }

    private final Supplier<DiameterMessageHeader> headerSupplier
            = () -> new DiameterMessageHeader.Builder(CommandCode.CC).setApplicationId(4)
            .setEndToEndId(RANDOM.nextLong())
            .setHopByHopId(RANDOM.nextLong())
            .setRequest()
            .setVersion((byte) 1)
            .build();

    private final BiFunction<DiameterMessageHeader, List<Avp>, Buffer> messageFunction = (header, dynamicAvps) -> {
        List<Avp> staticAvps = staticAvps();
        ArrayList<Avp> avps = new ArrayList<>(dynamicAvps.size() + staticAvps.size());
        avps.addAll(staticAvps);
        avps.addAll(dynamicAvps);

        return writeMessageToBuffer(new DiameterMessage(header, avps));
    };

    protected abstract List<Avp> staticAvps();

    private List<Avp> dynamicAvps(int requestType, boolean request, Avp grantedUnits) {
        HashMap<AvpCode, Avp> msccValue = new HashMap<>();
        msccValue.put(RATING_GROUP, ratingGroupAvp);
        if (request) {
            msccValue.put(REQUESTED_SERVICE_UNIT, emptyRequestAvp);
        }
        if (grantedUnits != null) {
            msccValue.put(USED_SERVICE_UNIT, usedServiceUnitsAvp(grantedUnits));
            msccValue.put(REPORTING_REASON, reportingReasonAvp);
        }
        return List.of(SESSION_ID.createAvp(session),
                       EVENT_TIMESTAMP.createAvp(ZonedDateTime.now()),
                       CC_REQUEST_NUMBER.createAvp(requestNumber - 1),
                       CC_REQUEST_TYPE.createAvp(requestType),
                       subscriptionIdAvp,
                       MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(msccValue));
    }

    protected abstract Avp usedServiceUnitsAvp(Avp grantedUnits);

    public static boolean validate(String flowName) {
        return switch (flowName) {
            case "ims-moc", "ims-mtc", "ps" -> true;
            default -> false;
        };
    }

    public static DiameterFlow newInstance(String flowName, int ratingGroup, int messageCount) {
        return switch (flowName) {
            case "ims-moc" -> new DiameterIMSMOC(ratingGroup, messageCount);
            case "ims-mtc" -> new DiameterIMSMTC(ratingGroup, messageCount);
            case "ps" -> new DiameterPS(ratingGroup, messageCount);
            default -> throw new IllegalStateException("Unexpected value: " + flowName);
        };
    }
}
