package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterCommandCode;
import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.DiameterMessageHeader;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCode;
import com.optiva.charging.openapi.diameter.avp.AvpCodeTable;

import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.optiva.DiameterLoadRunner.CALL_SLEEP;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.AUTH_APPLICATION_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.CALLED_STATION_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.CC_INPUT_OCTETS;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.CC_OUTPUT_OCTETS;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.CC_REQUEST_NUMBER;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.CC_REQUEST_TYPE;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.CC_TOTAL_OCTETS;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.DESTINATION_HOST;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.DESTINATION_REALM;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.EVENT_TIMESTAMP;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.GRANTED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.MULTIPLE_SERVICES_CREDIT_CONTROL;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.MULTIPLE_SERVICES_INDICATOR;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.ORIGIN_HOST;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.ORIGIN_REALM;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.RATING_GROUP;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.REQUESTED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SERVICE_CONTEXT_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SESSION_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SUBSCRIPTION_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SUBSCRIPTION_ID_DATA;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SUBSCRIPTION_ID_TYPE;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.USED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.TGPP.PS_INFORMATION;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.TGPP.REPORTING_REASON;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.TGPP.SERVICE_INFORMATION;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.TGPP.TGPP_USER_LOCATION_INFO;
import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

public class DiameterClient implements Runnable {
    private static final int[] ratingGroups = new int[]{16, 32};
    private static final String SERVICE_ID = "32251@3gpp.org";
    private static final Avp SERVICE_CONTEXT_ID_AVP = SERVICE_CONTEXT_ID.createAvp(SERVICE_ID);
    private static final Avp ORIGIN_HOST_AVP = ORIGIN_HOST.createAvp("diameterclient");
    private static final Avp ORIGIN_REALM_AVP = ORIGIN_REALM.createAvp("opencloud");
    private static final Avp DESTINATION_HOST_AVP = DESTINATION_HOST.createAvp("DestinationHost");
    private static final Avp DESTINATION_REALM_AVP = DESTINATION_REALM.createAvp("opencloud");
    private static final Avp AUTH_APPLICATION_ID_AVP = AUTH_APPLICATION_ID.createAvp(4);
    private static final Avp TYPE_CCR_AVP = CC_REQUEST_TYPE.createAvp(1);
    private static final Avp MULTIPLE_SERVICES_INDICATOR_AVP = MULTIPLE_SERVICES_INDICATOR.createAvp(1);
    private static final DiameterMessageHeader DIAMETER_MESSAGE_HEADER = new DiameterMessageHeader.Builder(
            DiameterCommandCode.CC).setApplicationId(4)
            .setEndToEndId(0x87b09775L)
            .setHopByHopId(0x00001c20)
            .setRequest()
            .setVersion((byte) 1)
            .build();
    private static final String CCR_T = "CCR-t";
    private static final String CCR_U = "CCR-u";
    private static final String CCR_I = "CCR-i";
    private static final ZonedDateTime NOW = ZonedDateTime.now();
    /////////////////////////////////////////////////////
    private final byte[] BYTE_ARR_BUFFER = new byte[4096];
    private final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(4096);
    private final DiameterLoadRunner loadRunner;
    private final BlockingQueue<Socket> socketQueue;
    private final AtomicLong counter;
    private final long subscriberRangeStart;
    private final int subscriberCount;
    private Logger logger;
    private int requestNumber = 0;
    private long grantLimit;
    private int ratingGroup;

    public DiameterClient(DiameterLoadRunner loadRunner,
                          BlockingQueue<Socket> socketQueue,
                          AtomicLong counter,
                          long subscriberRangeStart,
                          int subscriberCount) {
        this.loadRunner = loadRunner;
        this.socketQueue = socketQueue;
        this.counter = counter;
        this.subscriberRangeStart = subscriberRangeStart;
        this.subscriberCount = subscriberCount;
    }

    @Override
    public void run() {
        Socket ref = null;
        String session = "session-" + UUID.randomUUID();
        Random random = new Random();
        ratingGroup = ratingGroups[random.nextInt(ratingGroups.length)];
        String msisdn = Long.toString(subscriberRangeStart + random.nextLong(subscriberCount) + 1);
        logger = Logger.getLogger(Thread.currentThread().getName());
        try {
            Socket socket;
            ref = socket = socketQueue.take();
            long start = System.currentTimeMillis();
            boolean success = sendMsgAndWaitForAnswer(socket, ccrI(session, msisdn), CCR_I);
            sleep(CALL_SLEEP - System.currentTimeMillis() + start);
            ratingGroup = ratingGroups[random.nextInt(ratingGroups.length)];
            start = System.currentTimeMillis();
            success = success && sendMsgAndWaitForAnswer(socket, ccrU(session, msisdn, getChargeValue()), CCR_U);
            sleep(CALL_SLEEP - System.currentTimeMillis() + start);
            ratingGroup = ratingGroups[random.nextInt(ratingGroups.length)];
            start = System.currentTimeMillis();
            success = success && sendMsgAndWaitForAnswer(socket, ccrU(session, msisdn, getChargeValue()), CCR_U);
            sleep(CALL_SLEEP - System.currentTimeMillis() + start);
            ratingGroup = ratingGroups[random.nextInt(ratingGroups.length)];
            start = System.currentTimeMillis();
            success = success && sendMsgAndWaitForAnswer(socket, ccrT(session, msisdn, getChargeValue()), CCR_T);
            sleep(CALL_SLEEP - System.currentTimeMillis() + start);
        } catch (Exception e) {
            if (e.getCause() instanceof SocketException) {
                logger.severe("Socket closed, reconnecting | cause:" + e.getMessage());
                ref = loadRunner.initializeSocket();
            } else {
                logger.log(Level.SEVERE, "Unable to complete flow", e);
            }
        } finally {
            if (ref != null) {
                socketQueue.add(ref);
            }
        }
    }

    private void sleep(long l) throws InterruptedException {
        Thread.sleep(l <= 5
                     ? 10
                     : l - 5);
    }

    private long getChargeValue() {
        return 1000;
        //        return random.nextLong(grantLimit / 2, grantLimit) + 1;
    }

    private boolean sendMsgAndWaitForAnswer(Socket socket, byte[] req, String msgName) {
        try {
            socket.getOutputStream().write(req);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(msgName + " sent... ");
            }
            int len;
            DiameterMessage dm;
            while ((len = socket.getInputStream().read(BYTE_ARR_BUFFER)) > 19) {
                dm = new DiameterMessage(BYTE_ARR_BUFFER, len);
                if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.DW) || dm.getHeader()
                        .getCommandCode()
                        .equals(DiameterCommandCode.CE)) {
                    dm.getHeader().setRequest(false);
                    socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER));
                } else if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.CC)) {
                    Integer resultCode = dm.getAvp(AvpCodeTable.RFC.RESULT_CODE).getValue();
                    if (logger.isLoggable(Level.FINE)) {
                        String sessionId = dm.getAvp(SESSION_ID).getValue();
                        logger.fine("Answer received. SessionId: |" + sessionId);
                        logger.fine("Answer received. ResultCode: |" + resultCode);
                    }
                    if (resultCode != 2001) {
                        String error = dm.getAvp(AvpCodeTable.RFC.ERROR_MESSAGE).getValue();
                        logger.severe("Error(" + resultCode + ") | " + error);
                        return false;
                    } else if (!CCR_T.equals(msgName)) {
                        Avp avp = dm.<Map<AvpCode, Avp>>getAvpValue(MULTIPLE_SERVICES_CREDIT_CONTROL)
                                .get(GRANTED_SERVICE_UNIT);
                        Map<AvpCode, Avp> gsu = avp.getValue();
                        Avp to = gsu.get(CC_TOTAL_OCTETS);
                        grantLimit = to.getValue();
                    }
                    return true;
                } else {
                    throw new RuntimeException("Failure - unexpected: " + dm);
                }
            }
            throw new RuntimeException("Problem on received answer");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            counter.incrementAndGet();
        }
    }

    public byte[] ccrI(String sessionId, String msisdn) {
        List<Avp> avps = List.of(ORIGIN_HOST_AVP,
                                 ORIGIN_REALM_AVP,
                                 DESTINATION_HOST_AVP,
                                 DESTINATION_REALM_AVP,
                                 AUTH_APPLICATION_ID_AVP,
                                 SERVICE_CONTEXT_ID_AVP,
                                 TYPE_CCR_AVP,
                                 SERVICE_INFORMATION_AVP,
                                 MULTIPLE_SERVICES_INDICATOR_AVP,
                                 MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(Map.of(REQUESTED_SERVICE_UNIT,
                                                                                   REQUESTED_SERVICE_UNIT.createAvp(),
                                                                                   RATING_GROUP,
                                                                                   RATING_GROUP.createAvp(ratingGroup))),
                                 SESSION_ID.createAvp(sessionId),
                                 EVENT_TIMESTAMP.createAvp(NOW),
                                 CC_REQUEST_NUMBER.createAvp(requestNumber++),
                                 getSubscriptionIdAvp(msisdn));
        return new DiameterMessage(DIAMETER_MESSAGE_HEADER, avps).convertToBytes(BYTE_BUFFER);
    }

    public byte[] ccrU(String sessionId, String msisdn, long chargeValue) {
        long inputValue = (long) (chargeValue * 0.1);
        long outputValue = chargeValue - inputValue;
        List<Avp> avps = List.of(SESSION_ID.createAvp(sessionId),
                                 ORIGIN_HOST_AVP,
                                 ORIGIN_REALM_AVP,
                                 DESTINATION_HOST_AVP,
                                 DESTINATION_REALM_AVP,
                                 EVENT_TIMESTAMP.createAvp(NOW),
                                 AUTH_APPLICATION_ID_AVP,
                                 SERVICE_CONTEXT_ID_AVP,
                                 CC_REQUEST_TYPE.createAvp(2),
                                 CC_REQUEST_NUMBER.createAvp(requestNumber++),
                                 getSubscriptionIdAvp(msisdn),
                                 MULTIPLE_SERVICES_INDICATOR_AVP,
                                 SERVICE_INFORMATION_AVP,
                                 MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(Map.of(RATING_GROUP,
                                                                                   RATING_GROUP.createAvp(ratingGroup),
                                                                                   REPORTING_REASON,
                                                                                   REPORTING_REASON.createAvp(3),
                                                                                   //EXHAUSTED
                                                                                   USED_SERVICE_UNIT,
                                                                                   USED_SERVICE_UNIT.createAvp(Map.of(
                                                                                           CC_TOTAL_OCTETS,
                                                                                           CC_TOTAL_OCTETS.createAvp(
                                                                                                   chargeValue),
                                                                                           CC_INPUT_OCTETS,
                                                                                           CC_INPUT_OCTETS.createAvp(
                                                                                                   inputValue),
                                                                                           CC_OUTPUT_OCTETS,
                                                                                           CC_OUTPUT_OCTETS.createAvp(
                                                                                                   outputValue))),
                                                                                   REQUESTED_SERVICE_UNIT,
                                                                                   REQUESTED_SERVICE_UNIT.createAvp())));
        return new DiameterMessage(DIAMETER_MESSAGE_HEADER, avps).convertToBytes(BYTE_BUFFER);
    }

    public byte[] ccrT(String sessionId, String msisdn, long chargeValue) {
        long inputValue = (long) (chargeValue * 0.1);
        long outputValue = chargeValue - inputValue;
        List<Avp> avps = List.of(SESSION_ID.createAvp(sessionId),
                                 ORIGIN_HOST_AVP,
                                 ORIGIN_REALM_AVP,
                                 DESTINATION_HOST_AVP,
                                 DESTINATION_REALM_AVP,
                                 EVENT_TIMESTAMP.createAvp(NOW),
                                 AUTH_APPLICATION_ID_AVP,
                                 SERVICE_CONTEXT_ID_AVP,
                                 CC_REQUEST_TYPE.createAvp(3),
                                 CC_REQUEST_NUMBER.createAvp(requestNumber++),
                                 getSubscriptionIdAvp(msisdn),
                                 MULTIPLE_SERVICES_INDICATOR_AVP,
                                 SERVICE_INFORMATION_AVP,
                                 MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(Map.of(RATING_GROUP,
                                                                                   RATING_GROUP.createAvp(ratingGroup),
                                                                                   REPORTING_REASON,
                                                                                   REPORTING_REASON.createAvp(2),
                                                                                   USED_SERVICE_UNIT,
                                                                                   USED_SERVICE_UNIT.createAvp(Map.of(
                                                                                           CC_TOTAL_OCTETS,
                                                                                           CC_TOTAL_OCTETS.createAvp(
                                                                                                   chargeValue),
                                                                                           CC_INPUT_OCTETS,
                                                                                           CC_INPUT_OCTETS.createAvp(
                                                                                                   inputValue),
                                                                                           CC_OUTPUT_OCTETS,
                                                                                           CC_OUTPUT_OCTETS.createAvp(
                                                                                                   outputValue))))));
        return new DiameterMessage(DIAMETER_MESSAGE_HEADER, avps).convertToBytes(BYTE_BUFFER);
    }

    private static Avp getSubscriptionIdAvp(String msisdn) {
        return SUBSCRIPTION_ID.createAvp(Map.of(SUBSCRIPTION_ID_TYPE,
                                                SUBSCRIPTION_ID_TYPE.createAvp(0),
                                                SUBSCRIPTION_ID_DATA,
                                                SUBSCRIPTION_ID_DATA.createAvp(msisdn)));
    }

    public static final Avp SERVICE_INFORMATION_AVP = SERVICE_INFORMATION.createAvp(Map.of(PS_INFORMATION,
                                                                                           PS_INFORMATION.createAvp(Map.of(
                                                                                                   CALLED_STATION_ID,
                                                                                                   CALLED_STATION_ID.createAvp(
                                                                                                           "iot.truphone.com"),
                                                                                                   TGPP_USER_LOCATION_INFO,
                                                                                                   TGPP_USER_LOCATION_INFO.createAvp(
                                                                                                           parseHexBinary(
                                                                                                                   "0162f2102f4c6bb6"))))));
}
