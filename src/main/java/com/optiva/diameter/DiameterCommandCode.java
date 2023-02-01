package com.optiva.diameter;


import java.util.HashMap;
import java.util.Map;

public enum DiameterCommandCode {

    CE(257, "Capabilities Exchange"),
    RA(258, "Re Auth"),
    CC(272, "Credit Control"),
    AS(274, "Abort Session"),
    ST(275, "Session Termination"),
    DW(280, "Device Watchdog"),
    DP(282, "Disconnect Peer"),
    SL(8388635, "Spending Limit"),
    SN(8388636, "Spending Status Notification"),
    UNDEFINED(-1, "Undefined");

    private final int code;
    private final String name;

    private static final Map<Integer, DiameterCommandCode> valueMap;

    static {
        valueMap = new HashMap<>();
        for (final DiameterCommandCode cc : DiameterCommandCode.values()) {
            DiameterCommandCode.valueMap.put(cc.code, cc);
        }
    }

    DiameterCommandCode(final int code, final String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return this.code;
    }

    public String getName() {
        return name;
    }

    public static DiameterCommandCode getCommandCode(final int code) {
        if(valueMap.containsKey(code)){
            return valueMap.get(code);
        }
        return UNDEFINED;
    }

    @Override
    public String toString() {
        return name() + "-" + code;
    }
}
