package com.optiva.charging.openapi.diameter.common.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum ResultCode {
    /**
     * This informational error is returned by a Diameter server to
     * inform the access device that the authentication mechanism being
     * used requires multiple round trips, and a subsequent request needs
     * to be issued in order for access to be granted.
     */
    DIAMETER_MULTI_ROUND_AUTH(1001),
    /**
     * The request was successfully completed.
     */
    DIAMETER_SUCCESS(2001),
    /**
     * When returned, the request was successfully completed, but
     * additional processing is required by the application in order to
     * provide service to the user.
     */
    DIAMETER_LIMITED_SUCCESS(2002),
    /**
     * This error code is used when a Diameter entity receives a message
     * with a Command Code that it does not support.
     */
    DIAMETER_COMMAND_UNSUPPORTED(3001),
    /**
     * This error is given when Diameter cannot deliver the message to
     * the destination, either because no host within the realm
     * supporting the required application was available to process the
     * request or because the Destination-Host AVP was given without the
     * associated Destination-Realm AVP.
     */
    DIAMETER_UNABLE_TO_DELIVER(3002),
    /**
     * The intended realm of the request is not recognized.
     */
    DIAMETER_REALM_NOT_SERVED(3003),
    /**
     * When returned, a Diameter node SHOULD attempt to send the message
     * to an alternate peer.  This error MUST only be used when a
     * specific server is requested, and it cannot provide the requested
     * service.
     */
    DIAMETER_TOO_BUSY(3004),
    /**
     * An agent detected a loop while trying to get the message to the
     * intended recipient.  The message MAY be sent to an alternate peer,
     * if one is available, but the peer reporting the error has
     * identified a configuration problem.
     */
    DIAMETER_LOOP_DETECTED(3005),
    /**
     * A redirect agent has determined that the request could not be
     * satisfied locally, and the initiator of the request SHOULD direct
     * the request directly to the server, whose contact information has
     * been added to the response.  When set, the Redirect-Host AVP MUST
     * be present.
     */
    DIAMETER_REDIRECT_INDICATION(3006),
    /**
     * A request was sent for an application that is not supported.
     */
    DIAMETER_APPLICATION_UNSUPPORTED(3007),
    /**
     * A request was received whose bits in the Diameter header were set
     * either to an invalid combination or to a value that is
     * inconsistent with the Command Code's definition.
     */
    DIAMETER_INVALID_HDR_BITS(3008),
    /**
     * A request was received that included an AVP whose flag bits are
     * set to an unrecognized value or that is inconsistent with the
     * AVP's definition.
     */
    DIAMETER_INVALID_AVP_BITS(3009),
    /**
     * A CER was received from an unknown peer.
     */
    DIAMETER_UNKNOWN_PEER(3010),
    /**
     * The authentication process for the user failed, most likely due to
     * an invalid password used by the user.  Further attempts MUST only
     * be tried after prompting the user for a new password.
     */
    DIAMETER_AUTHENTICATION_REJECTED(4001),
    /**
     * A Diameter node received the accounting request but was unable to
     * commit it to stable storage due to a temporary lack of space.
     */
    DIAMETER_OUT_OF_SPACE(4002),
    /**
     * The peer has determined that it has lost the election process and
     * has therefore disconnected the transport connection.
     */
    ELECTION_LOST(4003),
    /**
     * The credit-control server denies the service request due to
     * service restrictions.  If the CCR contained used-service-units,
     * they are deducted, if possible.
     */
    DIAMETER_END_USER_SERVICE_DENIED(4010),
    /**
     * The credit-control server determines that the service can be
     * granted to the end user but that no further credit-control is
     * needed for the service (e.g., service is free of charge).
     */
    DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE(4011),
    /**
     * The credit-control server denies the service request because the
     * end user's account could not cover the requested service.  If the
     * CCR contained used-service-units they are deducted, if possible.
     */
    DIAMETER_CREDIT_LIMIT_REACHED(4012),
    /**
     * The peer received a message that contained an AVP that is not
     * recognized or supported and was marked with the 'M' (Mandatory)
     * bit.  A Diameter message with this error MUST contain one or more
     * Failed-AVP AVPs containing the AVPs that caused the failure.
     */
    DIAMETER_AVP_UNSUPPORTED(5001),
    /**
     * The request contained an unknown Session-Id.
     */
    DIAMETER_UNKNOWN_SESSION_ID(5002),
    /**
     * A request was received for which the user could not be authorized.
     * This error could occur if the service requested is not permitted
     * to the user.
     */
    DIAMETER_AUTHORIZATION_REJECTED(5003),
    /**
     * The request contained an AVP with an invalid value in its data
     * portion.  A Diameter message indicating this error MUST include
     * the offending AVPs within a Failed-AVP AVP.
     */
    DIAMETER_INVALID_AVP_VALUE(5004),
    /**
     * The request did not contain an AVP that is required by the Command
     * Code definition.  If this value is sent in the Result-Code AVP, a
     * Failed-AVP AVP SHOULD be included in the message.  The Failed-AVP
     * AVP MUST contain an example of the missing AVP complete with the
     * Vendor-Id if applicable.  The value field of the missing AVP
     * should be of correct minimum length and contain zeroes.
     */
    DIAMETER_MISSING_AVP(5005),
    /**
     * A request was received that cannot be authorized because the user
     * has already expended allowed resources.  An example of this error
     * condition is when a user that is restricted to one dial-up PPP
     * port attempts to establish a second PPP connection.
     */
    DIAMETER_RESOURCES_EXCEEDED(5006),
    /**
     * The Home Diameter server has detected AVPs in the request that
     * contradicted each other, and it is not willing to provide service
     * to the user.  The Failed-AVP AVP MUST be present, which contain
     * the AVPs that contradicted each other.
     */
    DIAMETER_CONTRADICTING_AVPS(5007),
    /**
     * A message was received with an AVP that MUST NOT be present.  The
     * Failed-AVP AVP MUST be included and contain a copy of the
     * offending AVP.
     */
    DIAMETER_AVP_NOT_ALLOWED(5008),
    /**
     * A message was received that included an AVP that appeared more
     * often than permitted in the message definition.  The Failed-AVP
     * AVP MUST be included and contain a copy of the first instance of
     * the offending AVP that exceeded the maximum number of occurrences.
     */
    DIAMETER_AVP_OCCURS_TOO_MANY_TIMES(5009),
    /**
     * This error is returned by a Diameter node that receives a CER
     * whereby no applications are common between the CER sending peer
     * and the CER receiving peer.
     */
    DIAMETER_NO_COMMON_APPLICATION(5010),
    /**
     * This error is returned when a request was received, whose version
     * number is unsupported.
     */
    DIAMETER_UNSUPPORTED_VERSION(5011),
    /**
     * This error is returned when a request is rejected for unspecified
     * reasons.
     */
    DIAMETER_UNABLE_TO_COMPLY(5012),
    /**
     * This error is returned when a reserved bit in the Diameter header
     * is set to one (1) or the bits in the Diameter header are set
     * incorrectly.
     */
    DIAMETER_INVALID_BIT_IN_HEADER(5013),
    /**
     * The request contained an AVP with an invalid length.  A Diameter
     * message indicating this error MUST include the offending AVPs
     * within a Failed-AVP AVP.  In cases where the erroneous AVP length
     * value exceeds the message length or is less than the minimum AVP
     * header length, it is sufficient to include the offending AVP
     * header and a zero filled payload of the minimum required length
     * for the payloads data type.  If the AVP is a Grouped AVP, the
     * Grouped AVP header with an empty payload would be sufficient to
     * indicate the offending AVP.  In the case where the offending AVP
     * header cannot be fully decoded when the AVP length is less than
     * the minimum AVP header length, it is sufficient to include an
     * offending AVP header that is formulated by padding the incomplete
     * AVP header with zero up to the minimum AVP header length.
     */
    DIAMETER_INVALID_AVP_LENGTH(5014),
    /**
     * This error is returned when a request is received with an invalid
     * message length.
     */
    DIAMETER_INVALID_MESSAGE_LENGTH(5015),
    /**
     * The request contained an AVP with which is not allowed to have the
     * given value in the AVP Flags field.  A Diameter message indicating
     * this error MUST include the offending AVPs within a Failed-AVP
     * AVP.
     */
    DIAMETER_INVALID_AVP_BIT_COMBO(5016),
    /**
     * This error is returned when a CER message is received, and there
     * are no common security mechanisms supported between the peers.  A
     * Capabilities-Exchange-Answer (CEA) message MUST be returned with
     * the Result-Code AVP set to DIAMETER_NO_COMMON_SECURITY.
     */
    DIAMETER_NO_COMMON_SECURITY(5017),
    /**
     * The specified end user is unknown in the credit-control server.
     */
    DIAMETER_USER_UNKNOWN(5030),
    /**
     * This error code is used to inform the credit-control client that
     * the credit-control server cannot rate the service request due to
     * insufficient rating input, an incorrect AVP combination, or an AVP
     * or an AVP value that is not recognized or supported in the rating.
     * The Failed-AVP AVP MUST be included and contain a copy of the
     * entire AVP(s) that could not be processed successfully or an
     * example of the missing AVP complete with the Vendor-Id if
     * applicable.  The value field of the missing AVP should be of
     * correct minimum length and contain zeros.
     */
    DIAMETER_RATING_FAILED(5031);

    private final int code;

    private static final Map<Integer, ResultCode> CODE_MAP = new HashMap<>();

    static {
        for (ResultCode rc : ResultCode.values()) {
            CODE_MAP.put(rc.code, rc);
        }
    }

    public static ResultCode fromCode(int code) {
        return CODE_MAP.get(code);
    }

    ResultCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    @Override
    public String toString() {
        return name() + " (" + code + ")";
    }
}