package com.optiva.diameter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static com.optiva.diameter.DiameterAvpDataType.*;

public final class DiameterAvpCodes {
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
        DiameterAvpCode AUTH_APPLICATION_ID = new DiameterAvpCode("AUTH_APPLICATION_ID", 258, VendorId.RFC, UNSIGNED_32);
        DiameterAvpCode CC_MONEY = new DiameterAvpCode("CC_MONEY", 413, VendorId.RFC, GROUPED);
        DiameterAvpCode CC_REQUEST_NUMBER = new DiameterAvpCode("CC_REQUEST_NUMBER", 415, VendorId.RFC, UNSIGNED_32);
        DiameterAvpCode CC_REQUEST_TYPE = new DiameterAvpCode("CC_REQUEST_TYPE", 416, VendorId.RFC, ENUMERATED);
        DiameterAvpCode CC_SERVICE_SPECIFIC_UNITS = new DiameterAvpCode("CC_SERVICE_SPECIFIC_UNITS", 417, VendorId.RFC, UNSIGNED_64);
        DiameterAvpCode DESTINATION_HOST = new DiameterAvpCode("DESTINATION_HOST", 293, VendorId.RFC, DIAMETER_IDENTITY);
        DiameterAvpCode DESTINATION_REALM = new DiameterAvpCode("DESTINATION_REALM", 283, VendorId.RFC, DIAMETER_IDENTITY);
        DiameterAvpCode DISCONNECT_CAUSE = new DiameterAvpCode("DISCONNECT_CAUSE", 273, VendorId.RFC, ENUMERATED);
        DiameterAvpCode ERROR_MESSAGE = new DiameterAvpCode("ERROR_MESSAGE", 281, VendorId.RFC, UTF8_STRING);
        DiameterAvpCode EVENT_TIMESTAMP = new DiameterAvpCode("EVENT_TIMESTAMP", 55, VendorId.RFC, TIME);
        DiameterAvpCode EXPERIMENTAL_RESULT = new DiameterAvpCode("EXPERIMENTAL_RESULT", 297, VendorId.RFC, GROUPED);
        DiameterAvpCode EXPERIMENTAL_RESULT_CODE = new DiameterAvpCode("EXPERIMENTAL_RESULT_CODE", 298, VendorId.RFC, UNSIGNED_32);
        DiameterAvpCode EXPONENT = new DiameterAvpCode("EXPONENT", 429, VendorId.RFC, INTEGER_32);
        DiameterAvpCode FAILED_AVP = new DiameterAvpCode("FAILED_AVP", 279, VendorId.RFC, GROUPED);
        DiameterAvpCode FIRMWARE_REVISION = new DiameterAvpCode("FIRMWARE_REVISION", 267, VendorId.RFC, UNSIGNED_32);
        DiameterAvpCode GRANTED_SERVICE_UNIT = new DiameterAvpCode("GRANTED_SERVICE_UNIT", 431, VendorId.RFC, GROUPED);
        DiameterAvpCode HOST_IP_ADDRESS = new DiameterAvpCode("HOST_IP_ADDRESS", 257, VendorId.RFC, ADDRESS);
        DiameterAvpCode INBAND_SECURITY_ID = new DiameterAvpCode("INBAND_SECURITY_ID", 299, VendorId.RFC, UNSIGNED_32);
        DiameterAvpCode MULTIPLE_SERVICES_CREDIT_CONTROL = new DiameterAvpCode("MULTIPLE_SERVICES_CREDIT_CONTROL", 456, VendorId.RFC, GROUPED);
        DiameterAvpCode MULTIPLE_SERVICES_INDICATOR = new DiameterAvpCode("MULTIPLE_SERVICES_INDICATOR", 455, VendorId.RFC, ENUMERATED);
        DiameterAvpCode ORIGIN_HOST = new DiameterAvpCode("ORIGIN_HOST", 264, VendorId.RFC, DIAMETER_IDENTITY);
        DiameterAvpCode ORIGIN_REALM = new DiameterAvpCode("ORIGIN_REALM", 296, VendorId.RFC, DIAMETER_IDENTITY);
        DiameterAvpCode PRODUCT_NAME = new DiameterAvpCode("PRODUCT_NAME", 269, VendorId.RFC, UTF8_STRING);
        DiameterAvpCode PROXY_HOST = new DiameterAvpCode("PROXY_HOST", 280, VendorId.RFC, DIAMETER_IDENTITY);
        DiameterAvpCode PROXY_INFO = new DiameterAvpCode("PROXY_INFO", 284, VendorId.RFC, GROUPED);
        DiameterAvpCode PROXY_STATE = new DiameterAvpCode("PROXY_STATE", 33, VendorId.RFC, OCTET_STRING);
        DiameterAvpCode REQUESTED_ACTION = new DiameterAvpCode("REQUEST_ACTION", 436, VendorId.RFC, ENUMERATED);
        DiameterAvpCode REQUESTED_SERVICE_UNIT = new DiameterAvpCode("REQUESTED_SERVICE_UNIT", 437, VendorId.RFC, GROUPED);
        DiameterAvpCode RESULT_CODE = new DiameterAvpCode("RESULT_CODE", 268, VendorId.RFC, UNSIGNED_32);
        DiameterAvpCode ROUTE_RECORD = new DiameterAvpCode("ROUTE_RECORD", 282, VendorId.RFC, DIAMETER_IDENTITY);
        DiameterAvpCode SERVICE_CONTEXT_ID = new DiameterAvpCode("SERVICE_CONTEXT_ID", 461, VendorId.RFC, UTF8_STRING);
        DiameterAvpCode SESSION_ID = new DiameterAvpCode("SESSION_ID", 263, VendorId.RFC, UTF8_STRING);
        DiameterAvpCode SUBSCRIPTION_ID = new DiameterAvpCode("SUBSCRIPTION_ID", 443, VendorId.RFC, GROUPED);
        DiameterAvpCode SUBSCRIPTION_ID_DATA = new DiameterAvpCode("SUBSCRIPTION_ID_DATA", 444, VendorId.RFC, UTF8_STRING);
        DiameterAvpCode SUBSCRIPTION_ID_TYPE = new DiameterAvpCode("SUBSCRIPTION_ID_TYPE", 450, VendorId.RFC, ENUMERATED);
        DiameterAvpCode SUPPORTED_VENDOR_ID = new DiameterAvpCode("SUPPORTED_VENDOR_ID", 265, VendorId.RFC, UNSIGNED_32);
        DiameterAvpCode TERMINATION_CAUSE = new DiameterAvpCode("TERMINATION_CAUSE", 295, VendorId.RFC, ENUMERATED);
        DiameterAvpCode UNIT_VALUE = new DiameterAvpCode("UNIT_VALUE", 445, VendorId.RFC, GROUPED);
        DiameterAvpCode USED_SERVICE_UNIT = new DiameterAvpCode("USED_SERVICE_UNIT", 446, VendorId.RFC, GROUPED);
        DiameterAvpCode VALUE_DIGITS = new DiameterAvpCode("VALUE_DIGITS", 447, VendorId.RFC, INTEGER_64);
        DiameterAvpCode VENDOR_ID = new DiameterAvpCode("VENDOR_ID", 266, VendorId.RFC, UNSIGNED_32);
        DiameterAvpCode VENDOR_SPECIFIC_APPLICATION_ID = new DiameterAvpCode("VENDOR_SPECIFIC_APPLICATION_ID", 260, VendorId.RFC, GROUPED);
    }

    public interface TGPP {
        DiameterAvpCode ADDITIONAL_CONTENT_INFORMATION = new DiameterAvpCode("ADDITIONAL_CONTENT_INFORMATION", 1207, VendorId.TGPP, GROUPED);
        DiameterAvpCode ADDRESS_DATA = new DiameterAvpCode("ADDRESS_DATA", 897, VendorId.TGPP, UTF8_STRING);
        DiameterAvpCode ADDRESS_DOMAIN = new DiameterAvpCode("ADDRESS_DOMAIN", 898, VendorId.TGPP, GROUPED);
        DiameterAvpCode ADDRESS_TYPE = new DiameterAvpCode("ADDRESS_TYPE", 899, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode APPLIC_ID = new DiameterAvpCode("APPLIC_ID", 1218, VendorId.TGPP, UTF8_STRING);
        DiameterAvpCode CLASS_IDENTIFIER = new DiameterAvpCode("CLASS_IDENTIFIER", 1214, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode CLIENT_ADDRESS = new DiameterAvpCode("CLIENT_ADDRESS", 2018, VendorId.TGPP, ADDRESS);
        DiameterAvpCode CONTENT_SIZE = new DiameterAvpCode("CONTENT_SIZE", 1206, VendorId.TGPP, UNSIGNED_32);
        DiameterAvpCode DELIVERY_REPORT_REQUESTED = new DiameterAvpCode("DELIVERY_REPORT_REQUESTED", 1216, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode DESTINATION_INTERFACE = new DiameterAvpCode("DESTINATION_INTERFACE", 2002, VendorId.TGPP, GROUPED);
        DiameterAvpCode IMSI_MCC_MNC = new DiameterAvpCode("IMSI_MCC_MNC", 8, VendorId.TGPP, OCTET_STRING);
        DiameterAvpCode INTERFACE_TYPE = new DiameterAvpCode("INTERFACE_TYPE", 2006, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode MESSAGE_CLASS = new DiameterAvpCode("MESSAGE_CLASS", 1213, VendorId.TGPP, GROUPED);
        DiameterAvpCode MESSAGE_ID = new DiameterAvpCode("MESSAGE_ID", 1210, VendorId.TGPP, UTF8_STRING);
        DiameterAvpCode MESSAGE_SIZE = new DiameterAvpCode("MESSAGE_SIZE", 1212, VendorId.TGPP, UNSIGNED_32);
        DiameterAvpCode MESSAGE_TYPE = new DiameterAvpCode("MESSAGE_TYPE", 1211, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode MMS_INFORMATION = new DiameterAvpCode("MMS_INFORMATION", 877, VendorId.TGPP, GROUPED);
        DiameterAvpCode MM_CONTENT_TYPE = new DiameterAvpCode("MM_CONTENT_TYPE", 1203, VendorId.TGPP, GROUPED);
        DiameterAvpCode ORIGINATING_SCCP_ADDRESS = new DiameterAvpCode("ORIGINATING_SCCP_ADDRESS", 2008, VendorId.TGPP, ADDRESS);
        DiameterAvpCode ORIGINATOR_ADDRESS = new DiameterAvpCode("ORIGINATOR_ADDRESS", 886, VendorId.TGPP, GROUPED);
        DiameterAvpCode ORIGINATOR_INTERFACE = new DiameterAvpCode("ORIGINATOR_INTERFACE", 2009, VendorId.TGPP, GROUPED);
        DiameterAvpCode READ_REPLY_REPORT_REQUESTED = new DiameterAvpCode("READ_REPLY_REPORT_REQUESTED", 1222, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode RECIPIENT_ADDRESS = new DiameterAvpCode("RECIPIENT_ADDRESS", 1201, VendorId.TGPP, GROUPED);
        DiameterAvpCode RECIPIENT_SCCP_ADDRESS = new DiameterAvpCode("RECIPIENT_SCCP_ADDRESS", 2010, VendorId.TGPP, ADDRESS);
        DiameterAvpCode SERVICE_INFORMATION = new DiameterAvpCode("SERVICE_INFORMATION", 873, VendorId.TGPP, GROUPED);
        DiameterAvpCode SL_REQUEST_TYPE = new DiameterAvpCode("SL_REQUEST_TYPE", 2904, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode SMSC_ADDRESS = new DiameterAvpCode("SMSC_ADDRESS", 2017, VendorId.TGPP, ADDRESS);
        DiameterAvpCode SMS_INFORMATION = new DiameterAvpCode("SMS_INFORMATION", 2000, VendorId.TGPP, GROUPED);
        DiameterAvpCode SMS_NODE = new DiameterAvpCode("SMS_NODE", 2016, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode SM_MESSAGE_TYPE = new DiameterAvpCode("SM_MESSAGE_TYPE", 2007, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode SM_USER_DATA_HEADER = new DiameterAvpCode("SM_USER_DATA_HEADER", 2015, VendorId.TGPP, OCTET_STRING);
        DiameterAvpCode SUBMISSION_TIME = new DiameterAvpCode("SUBMISSION_TIME", 1202, VendorId.TGPP, TIME);
        DiameterAvpCode TYPE_NUMBER = new DiameterAvpCode("TYPE_NUMBER", 1204, VendorId.TGPP, ENUMERATED);
        DiameterAvpCode VASP_ID = new DiameterAvpCode("VASP_ID", 1101, VendorId.TGPP, UTF8_STRING);
        DiameterAvpCode VAS_ID = new DiameterAvpCode("VASP_ID", 1102, VendorId.TGPP, UTF8_STRING);
    }

    public interface RKN {
        DiameterAvpCode AOC_PRICE = new DiameterAvpCode("AOC_PRICE", 123, VendorId.RKN, UNSIGNED_32);
        DiameterAvpCode SUBSCRIPTION_ID = new DiameterAvpCode("SUBSCRIPTION_ID", 111, VendorId.RKN, GROUPED);
        DiameterAvpCode SUBSCRIPTION_ID_DATA = new DiameterAvpCode("SUBSCRIPTION_ID_DATA", 113, VendorId.RKN, UTF8_STRING);
        DiameterAvpCode SUBSCRIPTION_ID_TYPE = new DiameterAvpCode("SUBSCRIPTION_ID_TYPE", 112, VendorId.RKN, INTEGER_32);
    }

    public interface NSN {
        DiameterAvpCode ACCESS_FRONTEND_ID = new DiameterAvpCode("ACCESS_FRONTEND_ID", 172, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode ACCOUNT = new DiameterAvpCode("ACCOUNT", 186, VendorId.NSN, GROUPED);
        DiameterAvpCode ACCOUNT_APPROVED = new DiameterAvpCode("ACCOUNT_APPROVED", 201, VendorId.NSN, UNSIGNED_32);
        DiameterAvpCode ACCOUNT_CURRENT_AUTHORIZED_AMOUNT = new DiameterAvpCode("ACCOUNT_CURRENT_AUTHORIZED_AMOUNT", 203, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ACCOUNT_CURRENT_BALANCE = new DiameterAvpCode("ACCOUNT_CURRENT_BALANCE", 202, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ACCOUNT_EXPIRY_DATE = new DiameterAvpCode("ACCOUNT_EXPIRY_DATE", 205, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ACCOUNT_ID = new DiameterAvpCode("ACCOUNT_ID", 198, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ACCOUNT_LAST_BALANCE_MOD_DATE = new DiameterAvpCode("ACCOUNT_LAST_BALANCE_MOD_DATE", 204, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ACCOUNT_OWNER_ID = new DiameterAvpCode("ACCOUNT_OWNER_ID", 200, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode ACCOUNT_TYPE = new DiameterAvpCode("ACCOUNT_TYPE", 199, VendorId.NSN, ENUMERATED);
        DiameterAvpCode BALANCE = new DiameterAvpCode("BALANCE", 126, VendorId.NSN, UNSIGNED_32);
        DiameterAvpCode CALCULATED_AMOUNT = new DiameterAvpCode("CALCULATED_AMOUNT", 206, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode CONSUMER_ACCOUNT_ID = new DiameterAvpCode("CONSUMER_ACCOUNT_ID", 174, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode CURRENCY = new DiameterAvpCode("CURRENCY", 175, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode DATE_OF_LAST_RECHARGE = new DiameterAvpCode("DATE_OF_LAST_RECHARGE", 181, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ERROR_CAUSE = new DiameterAvpCode("ERROR_CAUSE", 184, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ERROR_INFO = new DiameterAvpCode("ERROR_INFO", 179, VendorId.NSN, GROUPED);
        DiameterAvpCode ERROR_INFO_ERROR_ITEM = new DiameterAvpCode("ERROR_INFO_ERROR_ITEM", 192, VendorId.NSN, GROUPED);
        DiameterAvpCode ERROR_INFO_ERROR_ITEM_ERROR_ID = new DiameterAvpCode("ERROR_INFO_ERROR_ITEM_ERROR_ID", 194, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ERROR_INFO_ERROR_ITEM_ERROR_TEXT = new DiameterAvpCode("ERROR_INFO_ERROR_ITEM_ERROR_TEXT", 195, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode ERROR_INFO_ERROR_ITEM_FUNCTIONAL_UNIT_ID = new DiameterAvpCode("ERROR_INFO_ERROR_ITEM_FUNCTIONAL_UNIT_ID", 193, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ERROR_INFO_NO_MONEY_FLOW = new DiameterAvpCode("ERROR_INFO_NO_MONEY_FLOW", 191, VendorId.NSN, UNSIGNED_32);
        DiameterAvpCode EXPIRY_DATE = new DiameterAvpCode("EXPIRY_DATE", 180, VendorId.NSN, GROUPED);
        DiameterAvpCode EXPIRY_DATE_MODE = new DiameterAvpCode("EXPIRY_DATE_MODE", 196, VendorId.NSN, ENUMERATED);
        DiameterAvpCode EXPIRY_DATE_VALUE = new DiameterAvpCode("EXPIRY_DATE_VALUE", 197, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode MERCHANT_ID = new DiameterAvpCode("MERCHANT_ID", 173, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode METHOD_NAME = new DiameterAvpCode("METHOD_NAME", 178, VendorId.NSN, ENUMERATED);
        DiameterAvpCode NEW_EXPIRY_DATE = new DiameterAvpCode("NEW_EXPIRY_DATE", 183, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode OLD_EXPIRY_DATE = new DiameterAvpCode("OLD_EXPIRY_DATE", 182, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode ORIGINAL_CHARGE_TIME = new DiameterAvpCode("ORIGINAL_CHARGE_TIME", 185, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode PPI_INFORMATION = new DiameterAvpCode("PPI_INFORMATION", 102, VendorId.NSN, GROUPED);
        DiameterAvpCode PRODUCT_ID = new DiameterAvpCode("PRODUCT_ID", 170, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode PURPOSE = new DiameterAvpCode("PURPOSE", 171, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode RECIPIENT = new DiameterAvpCode("RECIPIENT", 224, VendorId.NSN, GROUPED);
        DiameterAvpCode REQUESTOR_CREDENTIALS = new DiameterAvpCode("REQUESTOR_CREDENTIALS", 207, VendorId.NSN, GROUPED);
        DiameterAvpCode REQUESTOR_PIN = new DiameterAvpCode("REQUESTOR_PIN", 212, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode REQUESTOR_ROLE = new DiameterAvpCode("REQUESTOR_ROLE", 210, VendorId.NSN, INTEGER_32);
        DiameterAvpCode REQUESTOR_USER_ID = new DiameterAvpCode("REQUESTOR_USER_ID", 211, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode ROUTING_INFO = new DiameterAvpCode("ROUTING_INFO", 208, VendorId.NSN, UTF8_STRING);
        DiameterAvpCode TIMEOUT = new DiameterAvpCode("TIMEOUT", 177, VendorId.NSN, GROUPED);
        DiameterAvpCode TIMEOUT_MODE = new DiameterAvpCode("TIMEOUT_MODE", 189, VendorId.NSN, ENUMERATED);
        DiameterAvpCode TIMEOUT_VALUE = new DiameterAvpCode("TIMEOUT_VALUE", 190, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode TIMESTAMP_FOR_RATING = new DiameterAvpCode("TIMESTAMP_FOR_RATING", 187, VendorId.NSN, UNSIGNED_64);
        DiameterAvpCode TRANSACTION_STATUS = new DiameterAvpCode("TRANSACTION_STATUS", 188, VendorId.NSN, ENUMERATED);
        DiameterAvpCode TRANSPARENT_DATA = new DiameterAvpCode("TRANSPARENT_DATA", 176, VendorId.NSN, UTF8_STRING);
    }

    public interface CHINATELECOM {
        DiameterAvpCode DA_SUBSCRIPTION_ID = new DiameterAvpCode("DA_SUBSCRIPTION_ID", 20512, VendorId.CHINATELECOM, GROUPED);
        DiameterAvpCode OA_SUBSCRIPTION_ID = new DiameterAvpCode("OA_SUBSCRIPTION_ID", 20511, VendorId.CHINATELECOM, GROUPED);
        DiameterAvpCode P2PSMS_INFORMATION = new DiameterAvpCode("P2PSMS_INFORMATION", 20400, VendorId.CHINATELECOM, GROUPED);
        DiameterAvpCode SMSC_ADDRESS = new DiameterAvpCode("SMSC_ADDRESS_HUAWEI", 20401, VendorId.CHINATELECOM, GROUPED);
        DiameterAvpCode SM_ID = new DiameterAvpCode("SM_ID", 20402, VendorId.CHINATELECOM, UTF8_STRING);
        DiameterAvpCode SM_LENGTH = new DiameterAvpCode("SM_LENGTH", 20403, VendorId.CHINATELECOM, UNSIGNED_32);
    }

    private static final Map<Integer, Map<Integer, DiameterAvpCode>> vendorMap = new HashMap<>();
    private static final String UNKNOWN = "UNKNOWN_";
    private static final String UNDERSCORE = "_";

    static {
        for (Class<?> vendorClass : DiameterAvpCodes.class.getDeclaredClasses()) {
            for (Field avpField : vendorClass.getDeclaredFields()) {
                if (Modifier.isStatic(avpField.getModifiers()) && avpField.getType().equals(DiameterAvpCode.class)) {
                    try {
                        final DiameterAvpCode avp = (DiameterAvpCode) avpField.get(DiameterAvpCode.class);
                        if (!vendorMap.containsKey(avp.getVendorId())) {
                            vendorMap.put(avp.getVendorId(), new HashMap<Integer, DiameterAvpCode>());
                        }
                        final Map<Integer, DiameterAvpCode> valueMap = vendorMap.get(avp.getVendorId());
                        valueMap.put(avp.getCode(), avp);
                    } catch (IllegalAccessException e) {
                        // todo
//                        EnvironmentImpl.getEnvironment()
//                                       .getTracer()
//                                       .topicError()
//                                       .errorApplicationCritical(DiameterAvpCode.class.getSimpleName(),
//                                               "static-init",
//                                               50,
//                                               null,
//                                               "error initiating avp code table",
//                                               e,
//                                               null);
                    }
                }
            }
        }
    }

    public static DiameterAvpCode get(final Integer vendorId, final Integer avpCode) {
        if (!vendorMap.containsKey(vendorId)) {
            vendorMap.put(vendorId, new HashMap<Integer, DiameterAvpCode>());
        }
        final Map<Integer, DiameterAvpCode> valueMap = vendorMap.get(vendorId);
        if (valueMap.containsKey(avpCode)) {
            return valueMap.get(avpCode);
        } else {
            final DiameterAvpCode avp = new DiameterAvpCode(UNKNOWN + vendorId + UNDERSCORE + avpCode,
                    avpCode,
                    vendorId,
                    OCTET_STRING);
            valueMap.put(avpCode, avp);
            return avp;
        }
    }
}
