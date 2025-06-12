package com.optiva.charging.openapi.diameter.exception;

public class DiameterAvpParseException extends RuntimeException {

    public DiameterAvpParseException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
