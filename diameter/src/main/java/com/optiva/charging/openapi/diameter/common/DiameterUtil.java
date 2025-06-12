package com.optiva.charging.openapi.diameter.common;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCode;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_CREDIT_CONTROL;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.MULTIPLE_SERVICES_INDICATOR;

public class DiameterUtil {
    public static Avp getChildAvp(DiameterMessage message, AvpCode... avpCodes) {
        if (avpCodes.length == 1) {
            return message.getAvp(avpCodes[0]);
        }
        Map<AvpCode, Avp> map = message.getAvpValue(avpCodes[0]);
        if (map == null) {
            return null;
        }
        return getChildAvp(map, 1, avpCodes);
    }

    private static Avp getChildAvp(Map<AvpCode, Avp> map, int startIndex, AvpCode... avpCodes) {
        int lastMapIndex = avpCodes.length - 1;
        for (int i = startIndex; i < lastMapIndex; i++) {
            Avp avp = map.get(avpCodes[i]);
            if (avp == null) {
                return null;
            }
            map = avp.getValue();
        }
        return map.get(avpCodes[lastMapIndex]);
    }

    public static <T> T getChildAvpValue(DiameterMessage message, AvpCode... avpCodes) {
        Avp avp = getChildAvp(message, avpCodes);
        return avp == null
               ? null
               : avp.getValue();
    }

    public static boolean isMultiService(DiameterMessage message) {
        return message.<Integer>getAvpValue(MULTIPLE_SERVICES_INDICATOR, 0).equals(1);
    }

    public static Avp getServiceAvp(DiameterMessage message, AvpCode... avpCodes) {
        boolean multiService = isMultiService(message);
        if (multiService) {
            Map<AvpCode, Avp> map = message.getAvpValue(MULTIPLE_SERVICES_CREDIT_CONTROL);
            if (map == null) {
                return null;
            }
            return getChildAvp(map, 0, avpCodes);
        } else {
            return getChildAvp(message, avpCodes);
        }
    }

    public static <T> T getServiceAvpValue(DiameterMessage message, AvpCode... avpCodes) {
        Avp avp = getServiceAvp(message, avpCodes);
        return avp == null
               ? null
               : avp.getValue();
    }

    public static Avp createGroupAvp(AvpCode code, Avp... avps) {
        if (avps == null || avps.length == 0) {
            return code.createAvp(Map.of());
        }
        return code.createAvp(Arrays.stream(avps).collect(Collectors.toMap(Avp::getAvpCode, a -> a)));
    }

    public static Map<AvpCode, Avp> createGroupAvpValue(Avp... avps) {
        if (avps == null || avps.length == 0) {
            return Map.of();
        }
        return Arrays.stream(avps).collect(Collectors.toMap(Avp::getAvpCode, a -> a));
    }
}
