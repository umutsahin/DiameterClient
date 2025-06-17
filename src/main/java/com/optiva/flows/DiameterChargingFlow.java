package com.optiva.flows;

import com.optiva.charging.openapi.diameter.avp.Avp;
import io.vertx.core.buffer.Buffer;

import java.util.UUID;

public abstract class DiameterChargingFlow extends DiameterFlow {
    protected final String msisdn;
    protected final int ratingGroup;
    protected final int messageCount;
    protected final String session;
    protected int requestNumber;
    protected Avp grantedUnits;

    public DiameterChargingFlow(String msisdn, int ratingGroup, int messageCount) {
        this.msisdn = msisdn;
        this.ratingGroup = ratingGroup;
        this.messageCount = messageCount;
        this.session = UUID.randomUUID().toString();
        this.requestNumber = 1;
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

    @Override
    public DiameterFlow restart() {
        return new DiameterIMS(msisdn, ratingGroup, messageCount);
    }

    protected abstract Buffer ccrIMessage();

    protected abstract Buffer ccrURequest();

    protected abstract Buffer ccrTRequest();

    public static boolean validate(String flowName) {
        return switch (flowName) {
            case "ims", "ps" -> true;
            default -> false;
        };
    }
}
