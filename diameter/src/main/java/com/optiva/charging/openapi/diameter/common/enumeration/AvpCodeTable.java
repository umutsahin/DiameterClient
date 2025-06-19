package com.optiva.charging.openapi.diameter.common.enumeration;

import com.optiva.charging.openapi.diameter.avp.AvpCode;
import com.optiva.charging.openapi.diameter.avp.AvpDataType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static com.optiva.charging.openapi.diameter.avp.AvpDataType.ADDRESS;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.ENUMERATED;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.GROUPED;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.IDENTITY;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.INTEGER_32;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.INTEGER_64;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.OCTET_STRING;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.TIME;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.UNSIGNED_32;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.UNSIGNED_64;
import static com.optiva.charging.openapi.diameter.avp.AvpDataType.UTF8_STRING;

@SuppressWarnings("unused")
public final class AvpCodeTable {
    public interface VendorId {
        int RFC = 0;
        int SIEMENS = 4329;
        int TGPP = 10415;
        int CISCO = 9;
        int NOKIA = 94;
        int NORTEL = 35;
        int VODAFONE = 12645;
        int NSN = 28458;
        int RKN = 5562;
        int CHINATELECOM = 81000;
        int INVALID = 0xFFFFFFFF;
    }

    public interface RFC {
        AvpCode USER_NAME = new AvpCode("USER_NAME", 1, VendorId.RFC, UTF8_STRING);
        AvpCode CALLED_STATION_ID = new AvpCode("CALLED_STATION_ID", 30, VendorId.RFC, UTF8_STRING);
        AvpCode PROXY_STATE = new AvpCode("PROXY_STATE", 33, VendorId.RFC, OCTET_STRING);
        AvpCode EVENT_TIMESTAMP = new AvpCode("EVENT_TIMESTAMP", 55, VendorId.RFC, TIME);
        AvpCode HOST_IP_ADDRESS = new AvpCode("HOST_IP_ADDRESS", 257, VendorId.RFC, ADDRESS);
        AvpCode AUTH_APPLICATION_ID = new AvpCode("AUTH_APPLICATION_ID", 258, VendorId.RFC, UNSIGNED_32);
        AvpCode ACCT_APPLICATION_ID = new AvpCode("ACCT_APPLICATION_ID", 259, VendorId.RFC, UNSIGNED_32);
        AvpCode VENDOR_SPECIFIC_APPLICATION_ID = new AvpCode("VENDOR_SPECIFIC_APPLICATION_ID",
                                                             260,
                                                             VendorId.RFC,
                                                             GROUPED);
        AvpCode SESSION_ID = new AvpCode("SESSION_ID", 263, VendorId.RFC, UTF8_STRING);
        AvpCode ORIGIN_HOST = new AvpCode("ORIGIN_HOST", 264, VendorId.RFC, IDENTITY);
        AvpCode SUPPORTED_VENDOR_ID = new AvpCode("SUPPORTED_VENDOR_ID", 265, VendorId.RFC, UNSIGNED_32);
        AvpCode VENDOR_ID = new AvpCode("VENDOR_ID", 266, VendorId.RFC, UNSIGNED_32);
        AvpCode FIRMWARE_REVISION = new AvpCode("FIRMWARE_REVISION", 267, VendorId.RFC, UNSIGNED_32);
        AvpCode RESULT_CODE = new AvpCode("RESULT_CODE", 268, VendorId.RFC, UNSIGNED_32);
        AvpCode PRODUCT_NAME = new AvpCode("PRODUCT_NAME", 269, VendorId.RFC, UTF8_STRING);
        AvpCode DISCONNECT_CAUSE = new AvpCode("DISCONNECT_CAUSE", 273, VendorId.RFC, ENUMERATED);
        AvpCode ORIGIN_STATE_ID = new AvpCode("ORIGIN_STATE_ID", 278, VendorId.RFC, UNSIGNED_32);
        AvpCode FAILED_AVP = new AvpCode("FAILED_AVP", 279, VendorId.RFC, GROUPED);
        AvpCode PROXY_HOST = new AvpCode("PROXY_HOST", 280, VendorId.RFC, IDENTITY);
        AvpCode ERROR_MESSAGE = new AvpCode("ERROR_MESSAGE", 281, VendorId.RFC, UTF8_STRING);
        AvpCode ROUTE_RECORD = new AvpCode("ROUTE_RECORD", 282, VendorId.RFC, IDENTITY);
        AvpCode DESTINATION_REALM = new AvpCode("DESTINATION_REALM", 283, VendorId.RFC, IDENTITY);
        AvpCode PROXY_INFO = new AvpCode("PROXY_INFO", 284, VendorId.RFC, GROUPED);
        AvpCode DESTINATION_HOST = new AvpCode("DESTINATION_HOST", 293, VendorId.RFC, IDENTITY);
        AvpCode TERMINATION_CAUSE = new AvpCode("TERMINATION_CAUSE", 295, VendorId.RFC, ENUMERATED);
        AvpCode ORIGIN_REALM = new AvpCode("ORIGIN_REALM", 296, VendorId.RFC, IDENTITY);
        AvpCode EXPERIMENTAL_RESULT = new AvpCode("EXPERIMENTAL_RESULT", 297, VendorId.RFC, GROUPED);
        AvpCode EXPERIMENTAL_RESULT_CODE = new AvpCode("EXPERIMENTAL_RESULT_CODE", 298, VendorId.RFC, UNSIGNED_32);
        AvpCode INBAND_SECURITY_ID = new AvpCode("INBAND_SECURITY_ID", 299, VendorId.RFC, UNSIGNED_32);
        AvpCode CC_INPUT_OCTETS = new AvpCode("CC_INPUT_OCTETS", 412, VendorId.RFC, UNSIGNED_64);
        AvpCode CC_MONEY = new AvpCode("CC_MONEY", 413, VendorId.RFC, GROUPED);
        AvpCode CC_OUTPUT_OCTETS = new AvpCode("CC_OUTPUT_OCTETS", 414, VendorId.RFC, UNSIGNED_64);
        AvpCode CC_REQUEST_NUMBER = new AvpCode("CC_REQUEST_NUMBER", 415, VendorId.RFC, UNSIGNED_32);
        AvpCode CC_REQUEST_TYPE = new AvpCode("CC_REQUEST_TYPE", 416, VendorId.RFC, ENUMERATED);
        AvpCode CC_SERVICE_SPECIFIC_UNITS = new AvpCode("CC_SERVICE_SPECIFIC_UNITS", 417, VendorId.RFC, UNSIGNED_64);
        AvpCode CC_TIME = new AvpCode("CC_TIME", 420, VendorId.RFC, UNSIGNED_32);
        AvpCode CC_TOTAL_OCTETS = new AvpCode("CC_TOTAL_OCTETS", 421, VendorId.RFC, UNSIGNED_64);
        AvpCode EXPONENT = new AvpCode("EXPONENT", 429, VendorId.RFC, INTEGER_32);
        AvpCode FINAL_UNIT_INDICATION = new AvpCode("FINAL_UNIT_INDICATION", 430, VendorId.RFC, GROUPED);
        AvpCode GRANTED_SERVICE_UNIT = new AvpCode("GRANTED_SERVICE_UNIT", 431, VendorId.RFC, GROUPED);
        AvpCode RATING_GROUP = new AvpCode("RATING_GROUP", 432, VendorId.RFC, UNSIGNED_32);
        AvpCode REDIRECT_SERVER = new AvpCode("REDIRECT_SERVER", 434, VendorId.RFC, GROUPED);
        AvpCode REQUESTED_ACTION = new AvpCode("REQUEST_ACTION", 436, VendorId.RFC, ENUMERATED);
        AvpCode REQUESTED_SERVICE_UNIT = new AvpCode("REQUESTED_SERVICE_UNIT", 437, VendorId.RFC, GROUPED);
        AvpCode SUBSCRIPTION_ID = new AvpCode("SUBSCRIPTION_ID", 443, VendorId.RFC, GROUPED);
        AvpCode SUBSCRIPTION_ID_DATA = new AvpCode("SUBSCRIPTION_ID_DATA", 444, VendorId.RFC, UTF8_STRING);
        AvpCode UNIT_VALUE = new AvpCode("UNIT_VALUE", 445, VendorId.RFC, GROUPED);
        AvpCode USED_SERVICE_UNIT = new AvpCode("USED_SERVICE_UNIT", 446, VendorId.RFC, GROUPED);
        AvpCode VALUE_DIGITS = new AvpCode("VALUE_DIGITS", 447, VendorId.RFC, AvpDataType.INTEGER_64);
        AvpCode VALIDITY_TIME = new AvpCode("VALIDITY_TIME", 448, VendorId.RFC, UNSIGNED_32);
        AvpCode FINAL_UNIT_ACTION = new AvpCode("FINAL_UNIT_ACTION", 449, VendorId.RFC, ENUMERATED);
        AvpCode SUBSCRIPTION_ID_TYPE = new AvpCode("SUBSCRIPTION_ID_TYPE", 450, VendorId.RFC, ENUMERATED);
        AvpCode TARIFF_TIME_CHANGE = new AvpCode("TARIFF_TIME_CHANGE", 451, VendorId.RFC, TIME);
        AvpCode MULTIPLE_SERVICES_INDICATOR = new AvpCode("MULTIPLE_SERVICES_INDICATOR", 455, VendorId.RFC, ENUMERATED);
        AvpCode MULTIPLE_SERVICES_CREDIT_CONTROL = new AvpCode("MULTIPLE_SERVICES_CREDIT_CONTROL",
                                                               456,
                                                               VendorId.RFC,
                                                               GROUPED);
        AvpCode USER_EQUIPMENT_INFO = new AvpCode("USER_EQUIPMENT_INFO", 458, VendorId.RFC, GROUPED);
        AvpCode USER_EQUIPMENT_INFO_TYPE = new AvpCode("USER_EQUIPMENT_INFO_TYPE", 459, VendorId.RFC, ENUMERATED);
        AvpCode USER_EQUIPMENT_INFO_VALUE = new AvpCode("USER_EQUIPMENT_INFO_VALUE", 460, VendorId.RFC, OCTET_STRING);
        AvpCode SERVICE_CONTEXT_ID = new AvpCode("SERVICE_CONTEXT_ID", 461, VendorId.RFC, UTF8_STRING);
        AvpCode RE_AUTH_REQUEST_TYPE = new AvpCode("RE_AUTH_REQUEST_TYPE", 285, VendorId.RFC, ENUMERATED);
    }

    public interface TGPP {
        AvpCode TGPP_CHARGING_ID = new AvpCode("TGPP_CHARGING_ID", 2, VendorId.TGPP, OCTET_STRING);
        AvpCode TGPP_PDP_TYPE = new AvpCode("TGPP_PDP_TYPE", 3, VendorId.TGPP, ENUMERATED);
        AvpCode TGPP_GPRS_NEGOTIATED_QOS_PROFILE = new AvpCode("TGPP_GPRS_NEGOTIATED_QOS_PROFILE",
                                                               5,
                                                               VendorId.TGPP,
                                                               UTF8_STRING);
        AvpCode IMSI_MCC_MNC = new AvpCode("IMSI_MCC_MNC", 8, VendorId.TGPP, OCTET_STRING);
        AvpCode TGPP_IMSI_MCC_MNC = new AvpCode("TGPP_IMSI_MCC_MNC", 8, VendorId.TGPP, UTF8_STRING);
        AvpCode TGPP_GGSN_MCC_MNC = new AvpCode("TGPP_GGSN_MCC_MNC", 9, VendorId.TGPP, UTF8_STRING);
        AvpCode TGPP_NSAPI = new AvpCode("TGPP_NSAPI", 10, VendorId.TGPP, OCTET_STRING);
        AvpCode TGPP_SELECTION_MODE = new AvpCode("TGPP_SELECTION_MODE", 12, VendorId.TGPP, UTF8_STRING);
        AvpCode TGPP_CHARGING_CHARACTERISTICS = new AvpCode("TGPP_CHARGING_CHARACTERISTICS",
                                                            13,
                                                            VendorId.TGPP,
                                                            UTF8_STRING);
        AvpCode TGPP_SGSN_MCC_MNC = new AvpCode("TGPP_SGSN_MCC_MNC", 18, VendorId.TGPP, UTF8_STRING);
        AvpCode TGPP_RAT_TYPE = new AvpCode("TGPP_RAT_TYPE", 21, VendorId.TGPP, OCTET_STRING);
        AvpCode TGPP_USER_LOCATION_INFO = new AvpCode("TGPP_USER_LOCATION_INFO", 22, VendorId.TGPP, OCTET_STRING);
        AvpCode TGPP_MS_TIME_ZONE = new AvpCode("TGPP_MS_TIME_ZONE", 23, VendorId.TGPP, OCTET_STRING);
        AvpCode CG_ADDRESS = new AvpCode("CG_ADDRESS", 846, VendorId.TGPP, ADDRESS);
        AvpCode GGSN_ADDRESS = new AvpCode("GGSN_ADDRESS", 847, VendorId.TGPP, ADDRESS);
        AvpCode TGPP_GGSN_ADDRESS = new AvpCode("TGPP_GGSN_ADDRESS", 7, VendorId.TGPP, OCTET_STRING);
        AvpCode QUOTA_HOLDING_TIME = new AvpCode("QUOTA_HOLDING_TIME", 871, VendorId.TGPP, UNSIGNED_32);
        AvpCode REPORTING_REASON = new AvpCode("TGPP_REPORTING_REASON", 872, VendorId.TGPP, ENUMERATED);
        AvpCode SERVICE_INFORMATION = new AvpCode("SERVICE_INFORMATION", 873, VendorId.TGPP, GROUPED);
        AvpCode PS_INFORMATION = new AvpCode("PS_INFORMATION", 874, VendorId.TGPP, GROUPED);
        AvpCode IMS_INFORMATION = new AvpCode("IMS_INFORMATION", 876, VendorId.TGPP, GROUPED);
        AvpCode EVENT_TYPE = new AvpCode("EVENT_TYPE", 823, VendorId.TGPP, GROUPED);
        AvpCode SIP_METHOD = new AvpCode("SIP_METHOD", 824, VendorId.TGPP, UTF8_STRING);
        AvpCode EVENT = new AvpCode("EVENT", 825, VendorId.TGPP, UTF8_STRING);
        AvpCode EXPIRES = new AvpCode("EXPIRES", 888, VendorId.TGPP, UNSIGNED_32);
        AvpCode ROLE_OF_NODE = new AvpCode("ROLE_OF_NODE", 829, VendorId.TGPP, ENUMERATED);
        AvpCode NODE_FUNCTIONALITY = new AvpCode("NODE_FUNCTIONALITY", 862, VendorId.TGPP, ENUMERATED);
        AvpCode USER_SESSION_ID = new AvpCode("USER_SESSION_ID", 830, VendorId.TGPP, UTF8_STRING);
        AvpCode OUTGOING_SESSION_ID = new AvpCode("OUTGOING_SESSION_ID", 2320, VendorId.TGPP, UTF8_STRING);
        AvpCode SESSION_PRIORITY = new AvpCode("SESSION_PRIORITY", 650, VendorId.TGPP, ENUMERATED);
        AvpCode CALLING_PARTY_ADDRESS = new AvpCode("CALLING_PARTY_ADDRESS", 831, VendorId.TGPP, UTF8_STRING);
        AvpCode CALLED_PARTY_ADDRESS = new AvpCode("CALLED_PARTY_ADDRESS", 832, VendorId.TGPP, UTF8_STRING);
        AvpCode CALLED_ASSERTED_IDENTITY = new AvpCode("CALLED_ASSERTED_IDENTITY", 1250, VendorId.TGPP, UTF8_STRING);
        AvpCode NUMBER_PORTABILITY_ROUTING_INFORMATION = new AvpCode("NUMBER_PORTABILITY_ROUTING_INFORMATION",
                                                                     2024,
                                                                     VendorId.TGPP,
                                                                     UTF8_STRING);
        AvpCode CARRIER_SELECT_ROUTING_INFORMATION = new AvpCode("CARRIER_SELECT_ROUTING_INFORMATION",
                                                                 2023,
                                                                 VendorId.TGPP,
                                                                 UTF8_STRING);
        AvpCode ALTERNATE_CHARGED_PARTY_ADDRESS = new AvpCode("ALTERNATE_CHARGED_PARTY_ADDRESS",
                                                              1280,
                                                              VendorId.TGPP,
                                                              UTF8_STRING);
        AvpCode REQUESTED_PARTY_ADDRESS = new AvpCode("REQUESTED_PARTY_ADDRESS", 1251, VendorId.TGPP, UTF8_STRING);
        AvpCode TIME_STAMPS = new AvpCode("TIME_STAMPS", 833, VendorId.TGPP, GROUPED);
        AvpCode SIP_REQUEST_TIMESTAMP = new AvpCode("SIP_REQUEST_TIMESTAMP", 834, VendorId.TGPP, TIME);
        AvpCode SIP_RESPONSE_TIMESTAMP = new AvpCode("SIP_RESPONSE_TIMESTAMP", 835, VendorId.TGPP, TIME);
        AvpCode SIP_REQUEST_TIMESTAMP_FRACTION = new AvpCode("SIP_REQUEST_TIMESTAMP_FRACTION",
                                                             2301,
                                                             VendorId.TGPP,
                                                             UNSIGNED_32);
        AvpCode SIP_RESPONSE_TIMESTAMP_FRACTION = new AvpCode("SIP_RESPONSE_TIMESTAMP_FRACTION",
                                                              2301,
                                                              VendorId.TGPP,
                                                              UNSIGNED_32);
        AvpCode APPLICATION_SERVER_INFORMATION = new AvpCode("APPLICATION_SERVER_INFORMATION",
                                                             850,
                                                             VendorId.TGPP,
                                                             GROUPED);
        AvpCode APPLICATION_SERVER = new AvpCode("APPLICATION_SERVER", 836, VendorId.TGPP, UTF8_STRING);
        AvpCode APPLICATION_PROVIDED_CALLED_PARTY_ADDRESS = new AvpCode("APPLICATION_PROVIDED_CALLED_PARTY_ADDRESS",
                                                                        837,
                                                                        VendorId.TGPP,
                                                                        UTF8_STRING);
        AvpCode INTER_OPERATOR_IDENTIFIER = new AvpCode("INTER_OPERATOR_IDENTIFIER", 838, VendorId.TGPP, GROUPED);
        AvpCode ORIGINATING_IOI = new AvpCode("ORIGINATING_IOI", 839, VendorId.TGPP, UTF8_STRING);
        AvpCode TERMINATING_IOI = new AvpCode("TERMINATING_IOI", 840, VendorId.TGPP, UTF8_STRING);
        AvpCode IMS_CHARGING_IDENTIFIER = new AvpCode("IMS_CHARGING_IDENTIFIER", 841, VendorId.TGPP, UTF8_STRING);
        AvpCode SDP_SESSION_DESCRIPTION = new AvpCode("SDP_SESSION_DESCRIPTION", 842, VendorId.TGPP, UTF8_STRING);
        AvpCode SDP_MEDIA_COMPONENT = new AvpCode("SDP_MEDIA_COMPONENT", 843, VendorId.TGPP, GROUPED);
        AvpCode SDP_MEDIA_NAME = new AvpCode("SDP_MEDIA_NAME", 844, VendorId.TGPP, UTF8_STRING);
        AvpCode SDP_MEDIA_DESCRIPTION = new AvpCode("SDP_MEDIA_DESCRIPTION", 845, VendorId.TGPP, UTF8_STRING);
        AvpCode LOCAL_GW_INSERTED_INDICATION = new AvpCode("LOCAL_GW_INSERTED_INDICATION",
                                                           2604,
                                                           VendorId.TGPP,
                                                           ENUMERATED);
        AvpCode IP_REALM_DEFAULT_INDICATION = new AvpCode("IP_REALM_DEFAULT_INDICATION",
                                                          2603,
                                                          VendorId.TGPP,
                                                          ENUMERATED);
        AvpCode TRANSCODER_INSERTED_INDICATION = new AvpCode("TRANSCODER_INSERTED_INDICATION",
                                                             2605,
                                                             VendorId.TGPP,
                                                             ENUMERATED);
        AvpCode MEDIA_INITIATOR_FLAG = new AvpCode("MEDIA_INITIATOR_FLAG", 882, VendorId.TGPP, ENUMERATED);
        AvpCode MEDIA_INITIATOR_PARTY = new AvpCode("MEDIA_INITIATOR_PARTY", 1288, VendorId.TGPP, UTF8_STRING);
        AvpCode ACCESS_NETWORK_CHARGING_IDENTIFIER_VALUE = new AvpCode("ACCESS_NETWORK_CHARGING_IDENTIFIER_VALUE",
                                                                       503,
                                                                       VendorId.TGPP,
                                                                       OCTET_STRING);
        AvpCode SDP_TYPE = new AvpCode("SDP_TYPE", 2036, VendorId.TGPP, ENUMERATED);
        AvpCode SERVED_PARTY_IP_ADDRESS = new AvpCode("SERVED_PARTY_IP_ADDRESS", 848, VendorId.TGPP, ADDRESS);
        AvpCode SERVER_CAPABILITIES = new AvpCode("SERVER_CAPABILITIES", 603, VendorId.TGPP, GROUPED);
        AvpCode MANDATORY_CAPABILITY = new AvpCode("MANDATORY_CAPABILITY", 604, VendorId.TGPP, UNSIGNED_32);
        AvpCode OPTIONAL_CAPABILITY = new AvpCode("OPTIONAL_CAPABILITY", 605, VendorId.TGPP, UNSIGNED_32);
        AvpCode SERVER_NAME = new AvpCode("SERVER_NAME", 602, VendorId.TGPP, UTF8_STRING);
        AvpCode BEARER_SERVICE = new AvpCode("BEARER_SERVICE", 854, VendorId.TGPP, OCTET_STRING);
        AvpCode SERVICE_ID = new AvpCode("SERVICE_ID", 855, VendorId.TGPP, UTF8_STRING);
        AvpCode SERVICE_SPECIFIC_INFO = new AvpCode("SERVICE_SPECIFIC_INFO", 1249, VendorId.TGPP, GROUPED);
        AvpCode SERVICE_SPECIFIC_DATA = new AvpCode("SERVICE_SPECIFIC_DATA", 863, VendorId.TGPP, UTF8_STRING);
        AvpCode SERVICE_SPECIFIC_TYPE = new AvpCode("SERVICE_SPECIFIC_TYPE", 1257, VendorId.TGPP, UNSIGNED_32);
        AvpCode MESSAGE_BODY = new AvpCode("MESSAGE_BODY", 889, VendorId.TGPP, GROUPED);
        AvpCode CONTENT_TYPE = new AvpCode("CONTENT_TYPE", 826, VendorId.TGPP, UTF8_STRING);
        AvpCode CONTENT_LENGTH = new AvpCode("CONTENT_LENGTH", 827, VendorId.TGPP, UNSIGNED_32);
        AvpCode CONTENT_DISPOSITION = new AvpCode("CONTENT_DISPOSITION", 828, VendorId.TGPP, UTF8_STRING);
        AvpCode ORIGINATOR = new AvpCode("ORIGINATOR", 864, VendorId.TGPP, ENUMERATED);
        AvpCode CAUSE_CODE = new AvpCode("CAUSE_CODE", 861, VendorId.TGPP, INTEGER_32);
        AvpCode ACCESS_NETWORK_INFORMATION = new AvpCode("ACCESS_NETWORK_INFORMATION",
                                                         1263,
                                                         VendorId.TGPP,
                                                         OCTET_STRING);
        AvpCode EARLY_MEDIA_DESCRIPTION = new AvpCode("EARLY_MEDIA_DESCRIPTION", 1272, VendorId.TGPP, GROUPED);
        AvpCode SDP_TIME_STAMPS = new AvpCode("SDP_TIME_STAMPS", 1273, VendorId.TGPP, GROUPED);
        AvpCode SDP_OFFER_TIMESTAMP = new AvpCode("SDP_OFFER_TIMESTAMP", 1274, VendorId.TGPP, TIME);
        AvpCode SDP_ANSWER_TIMESTAMP = new AvpCode("SDP_ANSWER_TIMESTAMP", 1275, VendorId.TGPP, TIME);
        AvpCode IMS_COMMUNICATION_SERVICE_IDENTIFIER = new AvpCode("IMS_COMMUNICATION_SERVICE_IDENTIFIER",
                                                                   1281,
                                                                   VendorId.TGPP,
                                                                   UTF8_STRING);
        AvpCode ONLINE_CHARGING_FLAG = new AvpCode("ONLINE_CHARGING_FLAG", 2303, VendorId.TGPP, ENUMERATED);
        AvpCode REAL_TIME_TARIFF_INFORMATION = new AvpCode("REAL_TIME_TARIFF_INFORMATION",
                                                           2305,
                                                           VendorId.TGPP,
                                                           GROUPED);
        AvpCode TARIFF_INFORMATION = new AvpCode("TARIFF_INFORMATION", 2060, VendorId.TGPP, GROUPED);
        AvpCode CURRENT_TARIFF = new AvpCode("CURRENT_TARIFF", 2056, VendorId.TGPP, GROUPED);
        AvpCode CURRENCY_CODE = new AvpCode("CURRENCY_CODE", 425, VendorId.TGPP, UNSIGNED_32);
        AvpCode SCALE_FACTOR = new AvpCode("SCALE_FACTOR", 2059, VendorId.TGPP, GROUPED);
        AvpCode VALUE_DIGITS = new AvpCode("VALUE_DIGITS", 447, VendorId.TGPP, INTEGER_64);
        AvpCode EXPONENT = new AvpCode("EXPONENT", 429, VendorId.TGPP, INTEGER_32);
        AvpCode RATE_ELEMENT = new AvpCode("RATE_ELEMENT", 2058, VendorId.TGPP, GROUPED);
        AvpCode CC_UNIT_TYPE = new AvpCode("CC_UNIT_TYPE", 454, VendorId.TGPP, ENUMERATED);
        AvpCode CHARGE_REASON_CODE = new AvpCode("CHARGE_REASON_CODE", 2118, VendorId.TGPP, ENUMERATED);
        AvpCode UNIT_VALUE = new AvpCode("UNIT_VALUE", 445, VendorId.TGPP, GROUPED);
        AvpCode UNIT_COST = new AvpCode("UNIT_COST", 2061, VendorId.TGPP, GROUPED);
        AvpCode UNIT_QUOTA_THRESHOLD = new AvpCode("UNIT_QUOTA_THRESHOLD", 1226, VendorId.TGPP, UNSIGNED_32);
        AvpCode NEXT_TARIFF = new AvpCode("NEXT_TARIFF", 2057, VendorId.TGPP, GROUPED);
        AvpCode TARIFF_XML = new AvpCode("TARIFF_XML", 2306, VendorId.TGPP, UTF8_STRING);
        AvpCode ACCOUNT_EXPIRATION = new AvpCode("ACCOUNT_EXPIRATION", 2309, VendorId.TGPP, TIME);
        AvpCode INITIAL_IMS_CHARGING_IDENTIFIER = new AvpCode("INITIAL_IMS_CHARGING_IDENTIFIER",
                                                              2321,
                                                              VendorId.TGPP,
                                                              UTF8_STRING);
        AvpCode ASSOCIATED_URI = new AvpCode("ASSOCIATED_URI", 856, VendorId.TGPP, UTF8_STRING);
        AvpCode MMS_INFORMATION = new AvpCode("MMS_INFORMATION", 877, VendorId.TGPP, GROUPED);
        AvpCode ORIGINATOR_ADDRESS = new AvpCode("ORIGINATOR_ADDRESS", 886, VendorId.TGPP, GROUPED);
        AvpCode ADDRESS_DATA = new AvpCode("ADDRESS_DATA", 897, VendorId.TGPP, UTF8_STRING);
        AvpCode ADDRESS_DOMAIN = new AvpCode("ADDRESS_DOMAIN", 898, VendorId.TGPP, GROUPED);
        AvpCode ADDRESS_TYPE = new AvpCode("ADDRESS_TYPE", 899, VendorId.TGPP, ENUMERATED);
        AvpCode CHARGING_RULE_BASE_NAME = new AvpCode("CHARGING_RULE_BASE_NAME", 1004, VendorId.TGPP, UTF8_STRING);
        AvpCode VASP_ID = new AvpCode("VASP_ID", 1101, VendorId.TGPP, UTF8_STRING);
        AvpCode VAS_ID = new AvpCode("VASP_ID", 1102, VendorId.TGPP, UTF8_STRING);
        AvpCode RECIPIENT_ADDRESS = new AvpCode("RECIPIENT_ADDRESS", 1201, VendorId.TGPP, GROUPED);
        AvpCode SUBMISSION_TIME = new AvpCode("SUBMISSION_TIME", 1202, VendorId.TGPP, TIME);
        AvpCode MM_CONTENT_TYPE = new AvpCode("MM_CONTENT_TYPE", 1203, VendorId.TGPP, GROUPED);
        AvpCode TYPE_NUMBER = new AvpCode("TYPE_NUMBER", 1204, VendorId.TGPP, ENUMERATED);
        AvpCode CONTENT_SIZE = new AvpCode("CONTENT_SIZE", 1206, VendorId.TGPP, UNSIGNED_32);
        AvpCode ADDITIONAL_CONTENT_INFORMATION = new AvpCode("ADDITIONAL_CONTENT_INFORMATION",
                                                             1207,
                                                             VendorId.TGPP,
                                                             GROUPED);
        AvpCode MESSAGE_ID = new AvpCode("MESSAGE_ID", 1210, VendorId.TGPP, UTF8_STRING);
        AvpCode MESSAGE_TYPE = new AvpCode("MESSAGE_TYPE", 1211, VendorId.TGPP, ENUMERATED);
        AvpCode MESSAGE_SIZE = new AvpCode("MESSAGE_SIZE", 1212, VendorId.TGPP, UNSIGNED_32);
        AvpCode MESSAGE_CLASS = new AvpCode("MESSAGE_CLASS", 1213, VendorId.TGPP, GROUPED);
        AvpCode CLASS_IDENTIFIER = new AvpCode("CLASS_IDENTIFIER", 1214, VendorId.TGPP, ENUMERATED);
        AvpCode DELIVERY_REPORT_REQUESTED = new AvpCode("DELIVERY_REPORT_REQUESTED", 1216, VendorId.TGPP, ENUMERATED);
        AvpCode APPLIC_ID = new AvpCode("APPLIC_ID", 1218, VendorId.TGPP, UTF8_STRING);
        AvpCode READ_REPLY_REPORT_REQUESTED = new AvpCode("READ_REPLY_REPORT_REQUESTED",
                                                          1222,
                                                          VendorId.TGPP,
                                                          ENUMERATED);
        AvpCode PDP_ADDRESS = new AvpCode("PDP_ADDRESS", 1227, VendorId.TGPP, ADDRESS);
        AvpCode SGSN_ADDRESS = new AvpCode("SGSN_ADDRESS", 1228, VendorId.TGPP, ADDRESS);
        AvpCode TGPP_SGSN_ADDRESS = new AvpCode("TGPP_SGSN_ADDRESS", 6, VendorId.TGPP, OCTET_STRING);
        AvpCode PDP_CONTEXT_TYPE = new AvpCode("PDP_CONTEXT_TYPE", 1247, VendorId.TGPP, ENUMERATED);
        AvpCode SMS_INFORMATION = new AvpCode("SMS_INFORMATION", 2000, VendorId.TGPP, GROUPED);
        AvpCode DESTINATION_INTERFACE = new AvpCode("DESTINATION_INTERFACE", 2002, VendorId.TGPP, GROUPED);
        AvpCode INTERFACE_TYPE = new AvpCode("INTERFACE_TYPE", 2006, VendorId.TGPP, ENUMERATED);
        AvpCode SM_MESSAGE_TYPE = new AvpCode("SM_MESSAGE_TYPE", 2007, VendorId.TGPP, ENUMERATED);
        AvpCode ORIGINATING_SCCP_ADDRESS = new AvpCode("ORIGINATING_SCCP_ADDRESS", 2008, VendorId.TGPP, ADDRESS);
        AvpCode ORIGINATOR_INTERFACE = new AvpCode("ORIGINATOR_INTERFACE", 2009, VendorId.TGPP, GROUPED);
        AvpCode RECIPIENT_SCCP_ADDRESS = new AvpCode("RECIPIENT_SCCP_ADDRESS", 2010, VendorId.TGPP, ADDRESS);
        AvpCode SM_USER_DATA_HEADER = new AvpCode("SM_USER_DATA_HEADER", 2015, VendorId.TGPP, OCTET_STRING);
        AvpCode SMS_NODE = new AvpCode("SMS_NODE", 2016, VendorId.TGPP, ENUMERATED);
        AvpCode SMSC_ADDRESS = new AvpCode("SMSC_ADDRESS", 2017, VendorId.TGPP, ADDRESS);
        AvpCode CLIENT_ADDRESS = new AvpCode("CLIENT_ADDRESS", 2018, VendorId.TGPP, ADDRESS);
        AvpCode PC_IDENTIFIER = new AvpCode("PC_IDENTIFIER", 2901, VendorId.TGPP, UTF8_STRING);
        AvpCode PC_STATUS = new AvpCode("PC_STATUS", 2902, VendorId.TGPP, UTF8_STRING);
        AvpCode PCS_REPORT = new AvpCode("PCS_REPORT", 2903, VendorId.TGPP, GROUPED);
        AvpCode SL_REQUEST_TYPE = new AvpCode("SL_REQUEST_TYPE", 2904, VendorId.TGPP, ENUMERATED);
    }

    public interface RKN {
        AvpCode AOC_PRICE = new AvpCode("AOC_PRICE", 123, VendorId.RKN, UNSIGNED_32);
        AvpCode SUBSCRIPTION_ID = new AvpCode("SUBSCRIPTION_ID", 111, VendorId.RKN, GROUPED);
        AvpCode SUBSCRIPTION_ID_DATA = new AvpCode("SUBSCRIPTION_ID_DATA", 113, VendorId.RKN, UTF8_STRING);
        AvpCode SUBSCRIPTION_ID_TYPE = new AvpCode("SUBSCRIPTION_ID_TYPE", 112, VendorId.RKN, INTEGER_32);
    }

    public interface NSN {
        AvpCode ACCESS_FRONTEND_ID = new AvpCode("ACCESS_FRONTEND_ID", 172, VendorId.NSN, UTF8_STRING);
        AvpCode ACCOUNT = new AvpCode("ACCOUNT", 186, VendorId.NSN, GROUPED);
        AvpCode ACCOUNT_APPROVED = new AvpCode("ACCOUNT_APPROVED", 201, VendorId.NSN, UNSIGNED_32);
        AvpCode ACCOUNT_CURRENT_AUTHORIZED_AMOUNT = new AvpCode("ACCOUNT_CURRENT_AUTHORIZED_AMOUNT",
                                                                203,
                                                                VendorId.NSN,
                                                                UNSIGNED_64);
        AvpCode ACCOUNT_CURRENT_BALANCE = new AvpCode("ACCOUNT_CURRENT_BALANCE", 202, VendorId.NSN, UNSIGNED_64);
        AvpCode ACCOUNT_EXPIRY_DATE = new AvpCode("ACCOUNT_EXPIRY_DATE", 205, VendorId.NSN, UNSIGNED_64);
        AvpCode ACCOUNT_ID = new AvpCode("ACCOUNT_ID", 198, VendorId.NSN, UNSIGNED_64);
        AvpCode ACCOUNT_LAST_BALANCE_MOD_DATE = new AvpCode("ACCOUNT_LAST_BALANCE_MOD_DATE",
                                                            204,
                                                            VendorId.NSN,
                                                            UNSIGNED_64);
        AvpCode ACCOUNT_OWNER_ID = new AvpCode("ACCOUNT_OWNER_ID", 200, VendorId.NSN, UTF8_STRING);
        AvpCode ACCOUNT_TYPE = new AvpCode("ACCOUNT_TYPE", 199, VendorId.NSN, ENUMERATED);
        AvpCode BALANCE = new AvpCode("BALANCE", 126, VendorId.NSN, UNSIGNED_32);
        AvpCode CALCULATED_AMOUNT = new AvpCode("CALCULATED_AMOUNT", 206, VendorId.NSN, UNSIGNED_64);
        AvpCode CONSUMER_ACCOUNT_ID = new AvpCode("CONSUMER_ACCOUNT_ID", 174, VendorId.NSN, UNSIGNED_64);
        AvpCode CURRENCY = new AvpCode("CURRENCY", 175, VendorId.NSN, UTF8_STRING);
        AvpCode DATE_OF_LAST_RECHARGE = new AvpCode("DATE_OF_LAST_RECHARGE", 181, VendorId.NSN, UNSIGNED_64);
        AvpCode ERROR_CAUSE = new AvpCode("ERROR_CAUSE", 184, VendorId.NSN, UNSIGNED_64);
        AvpCode ERROR_INFO = new AvpCode("ERROR_INFO", 179, VendorId.NSN, GROUPED);
        AvpCode ERROR_INFO_ERROR_ITEM = new AvpCode("ERROR_INFO_ERROR_ITEM", 192, VendorId.NSN, GROUPED);
        AvpCode ERROR_INFO_ERROR_ITEM_ERROR_ID = new AvpCode("ERROR_INFO_ERROR_ITEM_ERROR_ID",
                                                             194,
                                                             VendorId.NSN,
                                                             UNSIGNED_64);
        AvpCode ERROR_INFO_ERROR_ITEM_ERROR_TEXT = new AvpCode("ERROR_INFO_ERROR_ITEM_ERROR_TEXT",
                                                               195,
                                                               VendorId.NSN,
                                                               UTF8_STRING);
        AvpCode ERROR_INFO_ERROR_ITEM_FUNCTIONAL_UNIT_ID = new AvpCode("ERROR_INFO_ERROR_ITEM_FUNCTIONAL_UNIT_ID",
                                                                       193,
                                                                       VendorId.NSN,
                                                                       UNSIGNED_64);
        AvpCode ERROR_INFO_NO_MONEY_FLOW = new AvpCode("ERROR_INFO_NO_MONEY_FLOW", 191, VendorId.NSN, UNSIGNED_32);
        AvpCode EXPIRY_DATE = new AvpCode("EXPIRY_DATE", 180, VendorId.NSN, GROUPED);
        AvpCode EXPIRY_DATE_MODE = new AvpCode("EXPIRY_DATE_MODE", 196, VendorId.NSN, ENUMERATED);
        AvpCode EXPIRY_DATE_VALUE = new AvpCode("EXPIRY_DATE_VALUE", 197, VendorId.NSN, UNSIGNED_64);
        AvpCode MERCHANT_ID = new AvpCode("MERCHANT_ID", 173, VendorId.NSN, UTF8_STRING);
        AvpCode METHOD_NAME = new AvpCode("METHOD_NAME", 178, VendorId.NSN, ENUMERATED);
        AvpCode NEW_EXPIRY_DATE = new AvpCode("NEW_EXPIRY_DATE", 183, VendorId.NSN, UNSIGNED_64);
        AvpCode OLD_EXPIRY_DATE = new AvpCode("OLD_EXPIRY_DATE", 182, VendorId.NSN, UNSIGNED_64);
        AvpCode ORIGINAL_CHARGE_TIME = new AvpCode("ORIGINAL_CHARGE_TIME", 185, VendorId.NSN, UNSIGNED_64);
        AvpCode PPI_INFORMATION = new AvpCode("PPI_INFORMATION", 102, VendorId.NSN, GROUPED);
        AvpCode PRODUCT_ID = new AvpCode("PRODUCT_ID", 170, VendorId.NSN, UTF8_STRING);
        AvpCode PURPOSE = new AvpCode("PURPOSE", 171, VendorId.NSN, UTF8_STRING);
        AvpCode RECIPIENT = new AvpCode("RECIPIENT", 224, VendorId.NSN, GROUPED);
        AvpCode REQUESTOR_CREDENTIALS = new AvpCode("REQUESTOR_CREDENTIALS", 207, VendorId.NSN, GROUPED);
        AvpCode REQUESTOR_PIN = new AvpCode("REQUESTOR_PIN", 212, VendorId.NSN, UTF8_STRING);
        AvpCode REQUESTOR_ROLE = new AvpCode("REQUESTOR_ROLE", 210, VendorId.NSN, INTEGER_32);
        AvpCode REQUESTOR_USER_ID = new AvpCode("REQUESTOR_USER_ID", 211, VendorId.NSN, UTF8_STRING);
        AvpCode ROUTING_INFO = new AvpCode("ROUTING_INFO", 208, VendorId.NSN, UTF8_STRING);
        AvpCode TIMEOUT = new AvpCode("TIMEOUT", 177, VendorId.NSN, GROUPED);
        AvpCode TIMEOUT_MODE = new AvpCode("TIMEOUT_MODE", 189, VendorId.NSN, ENUMERATED);
        AvpCode TIMEOUT_VALUE = new AvpCode("TIMEOUT_VALUE", 190, VendorId.NSN, UNSIGNED_64);
        AvpCode TIMESTAMP_FOR_RATING = new AvpCode("TIMESTAMP_FOR_RATING", 187, VendorId.NSN, UNSIGNED_64);
        AvpCode TRANSACTION_STATUS = new AvpCode("TRANSACTION_STATUS", 188, VendorId.NSN, ENUMERATED);
        AvpCode TRANSPARENT_DATA = new AvpCode("TRANSPARENT_DATA", 176, VendorId.NSN, UTF8_STRING);
    }

    public interface ChinaTelecom {
        AvpCode DA_SUBSCRIPTION_ID = new AvpCode("DA_SUBSCRIPTION_ID", 20512, VendorId.CHINATELECOM, GROUPED);
        AvpCode OA_SUBSCRIPTION_ID = new AvpCode("OA_SUBSCRIPTION_ID", 20511, VendorId.CHINATELECOM, GROUPED);
        AvpCode P2PSMS_INFORMATION = new AvpCode("P2PSMS_INFORMATION", 20400, VendorId.CHINATELECOM, GROUPED);
        AvpCode SMSC_ADDRESS = new AvpCode("SMSC_ADDRESS_HUAWEI", 20401, VendorId.CHINATELECOM, GROUPED);
        AvpCode SM_ID = new AvpCode("SM_ID", 20402, VendorId.CHINATELECOM, UTF8_STRING);
        AvpCode SM_LENGTH = new AvpCode("SM_LENGTH", 20403, VendorId.CHINATELECOM, UNSIGNED_32);
    }

    public interface Vodafone {
        AvpCode USER_LOCATION_INFORMATION = new AvpCode("USER_LOCATION_INFORMATION",
                                                        267,
                                                        VendorId.VODAFONE,
                                                        OCTET_STRING);
    }

    private static final Map<Integer, Map<Integer, AvpCode>> vendorMap = new HashMap<>();
    private static final String UNKNOWN = "UNKNOWN_";
    private static final String UNDERSCORE = "_";

    static {
        for (Class<?> vendorClass : AvpCodeTable.class.getDeclaredClasses()) {
            for (Field avpField : vendorClass.getDeclaredFields()) {
                if (Modifier.isStatic(avpField.getModifiers()) && avpField.getType().equals(AvpCode.class)) {
                    try {
                        final AvpCode avp = (AvpCode) avpField.get(AvpCode.class);
                        if (!vendorMap.containsKey(avp.getVendorId())) {
                            vendorMap.put(avp.getVendorId(), new HashMap<Integer, AvpCode>());
                        }
                        final Map<Integer, AvpCode> valueMap = vendorMap.get(avp.getVendorId());
                        valueMap.put(avp.getCode(), avp);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static AvpCode get(final Integer vendorId, final Integer avpCode) {
        if (!vendorMap.containsKey(vendorId)) {
            vendorMap.put(vendorId, new HashMap<>());
        }
        final Map<Integer, AvpCode> valueMap = vendorMap.get(vendorId);
        if (valueMap.containsKey(avpCode)) {
            return valueMap.get(avpCode);
        } else {
            final AvpCode avp = new AvpCode(UNKNOWN + vendorId + UNDERSCORE + avpCode, avpCode, vendorId, OCTET_STRING);
            valueMap.put(avpCode, avp);
            return avp;
        }
    }
}
