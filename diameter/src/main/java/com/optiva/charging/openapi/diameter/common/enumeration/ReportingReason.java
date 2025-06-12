package com.optiva.charging.openapi.diameter.common.enumeration;

import java.util.HashMap;
import java.util.Map;

/**
 * 3GPP TS 32.299 version 12.6.0 Release 12 137 ETSI TS 132 299 V12.6.0 (2014-10)
 * <p>
 * 7.2.175 Reporting-Reason AVP
 * The Reporting-Reason AVP (AVP code 872) is of type Enumerated and specifies the reason for usage reporting for one or more types of quota for a particular category. It can occur directly in the Multiple-Services-Credit-Control AVP, or in the Used-Service-Units AVP within a CCR command reporting credit usage. It shall not be used at command level. It shall always and shall only be sent when usage is being reported. The following values are defined for the Reporting- Reason AVP:
 * 0 THRESHOLD
 * This value is used to indicate that the reason for usage reporting of the particular quota type indicated in the Used-Service-Units AVP where it appears is that the threshold has been reached.
 * 1 QHT
 * This value is used to indicate that the reason for usage reporting of all quota types of the Multiple-Service- Credit-Control AVP where its appears is that the quota holding time specified in a previous CCA command has been hit (i.e. the quota has been unused for that period of time).
 * 2 FINAL
 * This value is used to indicate that the reason for usage reporting of all quota types of the Multiple-Service- Credit-Control AVP where its appears is that a service termination has happened, e.g. PDP context, IP CAN bearer termination or service data flow termination.
 * 3 QUOTA_EXHAUSTED
 * This value is used to indicate that the reason for usage reporting of the particular quota type indicated in the Used-Service-Units AVP where it appears is that the quota has been exhausted.
 * 4 VALIDITY_TIME
 * This value is used to indicate that the reason for usage reporting of all quota types of the Multiple-Service- Credit-Control AVP where its appears is that the credit authorization lifetime provided in the Validity-Time AVP has expired.
 * 5 OTHER_QUOTA_TYPE
 * This value is used to indicate that the reason for usage reporting of the particular quota type indicated in the Used-Service-Units AVP where it appears is that, for a multi-dimensional quota, one reached a trigger condition and the other quota is being reported.
 * 6 RATING_CONDITION_CHANGE
 * This value is used to indicate that the reason for usage reporting of all quota types of the Multiple-Service- Credit-Control AVP where its appears is that a change has happened in some of the rating conditions that were previously armed (through the Trigger AVP, e.g. QoS, Radio Access Technology,...). The specific conditions that have changed are indicated in an associated Trigger AVP.
 * 7 FORCED_REAUTHORISATION
 * This value is used to indicate that the reason for usage reporting of all quota types of the Multiple-Service- Credit-Control AVP where its appears is that it is there has been a Server initiated re-authorization procedure, i.e. receipt of RAR command
 * 8 POOL_EXHAUSTED
 * This value is used to indicate that the reason for usage reporting of the particular quota type indicated in the Used-Service-Units AVP where it appears is that granted units are still available in the pool but are not sufficient for a rating group using the pool.
 * The values QHT, FINAL, VALIDITY_TIME, FORCED_REAUTHORISATION, RATING_CONDITION_CHANGE apply for all quota types and are used directly in the Multiple-Services-Credit-Control AVP, whereas the values THRESHOLD, QUOTA_EXHAUSTED and OTHER_QUOTA_TYPE apply to one particular quota type and shall occur only in the Used-Service-Units AVP. The value POOL_EXHAUSTED apply to all quota types using the credit pool and occurs in the Used-Service-Units AVP. It may optionally occur in the Multiple-Services-Credit-Control AVP if all quota types use the same pool.
 * When the value RATING_CONDITION_CHANGE is used, the Trigger AVP shall also be included to indicate the specific events which caused the re-authorization request.
 */
public enum ReportingReason {
    THRESHOLD,
    QHT,
    FINAL,
    QUOTA_EXHAUSTED,
    VALIDITY_TIME,
    OTHER_QUOTA_TYPE,
    RATING_CONDITION_CHANGE,
    FORCED_REAUTHORISATION,
    POOL_EXHAUSTED;

    private static final Map<Integer, ReportingReason> MAP = new HashMap<>();

    static {
        for (ReportingReason reason : ReportingReason.values()) {
            MAP.put(reason.ordinal(), reason);
        }
    }

    public static ReportingReason get(int ordinal) {
        return MAP.get(ordinal);
    }
}
