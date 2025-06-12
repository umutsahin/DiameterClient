package com.optiva.charging.openapi.diameter.exception;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.common.enumeration.ResultCode;

public class DiameterValidationException extends RuntimeException {
    private final DiameterMessage diameterMessage;
    private final Avp avp;
    private final ResultCode resultCode;

    public DiameterValidationException(String message,
                                       DiameterMessage diameterMessage,
                                       Avp avp,
                                       ResultCode resultCode) {
        super(message);
        this.diameterMessage = diameterMessage;
        this.avp = avp;
        this.resultCode = resultCode;
    }

    public DiameterMessage getDiameterMessage() {
        return diameterMessage;
    }

    public Avp getAvp() {
        return avp;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
