package com.optiva.charging.openapi.diameter.common.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum CCRequestType {
    CCI(1, "INITIAL_REQUEST"),
    CCU(2, "UPDATE_REQUEST"),
    CCT(3, "TERMINATION_REQUEST"),
    CCE(4, "EVENT_REQUEST");

    private final int typeCode;
    private final String typeName;

    private static final Map<Integer, CCRequestType> valueMap;

    static {
        valueMap = new HashMap<>();
        for (final CCRequestType cc : CCRequestType.values()) {
            CCRequestType.valueMap.put(cc.typeCode, cc);
        }
    }

    CCRequestType(final int typeCode, final String typeName) {
        this.typeCode = typeCode;
        this.typeName = typeName;
    }

    public int getTypeCode() {
        return typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public static CCRequestType fromTypeCode(int typeCode) {
        if (valueMap.containsKey(typeCode)) {
            return valueMap.get(typeCode);
        }
        throw new IllegalArgumentException("Invalid request type code: " + typeCode);
    }
}
