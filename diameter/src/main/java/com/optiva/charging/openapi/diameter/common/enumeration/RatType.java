package com.optiva.charging.openapi.diameter.common.enumeration;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("java:S115")
public enum RatType {
    Reserved,
    UTRAN,
    GERAN,
    WLAN,
    GAN,
    HSPA_Evolution,
    EUTRAN,
    Virtual;

    private static final Map<Integer, RatType> MAP = new HashMap<>();

    static {
        for (RatType reason : RatType.values()) {
            MAP.put(reason.ordinal(), reason);
        }
    }

    public static RatType get(int ordinal) {
        return MAP.get(ordinal);
    }
}
