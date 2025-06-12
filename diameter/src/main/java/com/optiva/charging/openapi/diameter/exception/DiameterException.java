package com.optiva.charging.openapi.diameter.exception;

import com.optiva.charging.openapi.diameter.DiameterMessage;

public class DiameterException extends RuntimeException {
    private final DiameterMessage dm;
    private final int resultCode;

    public DiameterException(String message, Throwable cause, int resultCode) {
        super(message, cause);
        this.resultCode = resultCode;
        dm = null;
    }

    public DiameterException(String message, DiameterMessage dm, int resultCode) {
        super(message);
        this.dm = dm;
        this.resultCode = resultCode;
    }

    public DiameterException(String message, Throwable cause, DiameterMessage dm, int resultCode) {
        super(message, cause);
        this.dm = dm;
        this.resultCode = resultCode;
    }

    public DiameterMessage getDm() {
        return dm;
    }

    public int getResultCode() {
        return resultCode;
    }
}
