package com.optiva.charging.openapi.diameter.common.enumeration;

public enum ServiceType {
    FBC("FBCD"),
    MMS("MMSD"),
    IMS("IMSD"),
    POC("POCD"),
    SMS("SMSD"),
    GEX("GEX"),
    CAR("CAR");

    private final String subServiceType;

    ServiceType(String subServiceType) {
        this.subServiceType = subServiceType;
    }

    public String getSubServiceType() {
        return subServiceType;
    }
}
