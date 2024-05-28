package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterCommandCode;
import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.DiameterMessageHeader;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCodeTable;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.HOST_IP_ADDRESS;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.MULTIPLE_SERVICES_CREDIT_CONTROL;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.MULTIPLE_SERVICES_INDICATOR;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.ORIGIN_HOST;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.ORIGIN_REALM;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.PRODUCT_NAME;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.RATING_GROUP;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.REQUESTED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SERVICE_CONTEXT_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SESSION_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SUBSCRIPTION_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SUBSCRIPTION_ID_DATA;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.SUBSCRIPTION_ID_TYPE;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.USED_SERVICE_UNIT;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.VENDOR_ID;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.TGPP.PS_INFORMATION;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.TGPP.REPORTING_REASON;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.TGPP.SERVICE_INFORMATION;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.TGPP.TGPP_USER_LOCATION_INFO;
import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

public class DiameterClient implements Callable<Void> {
    private static final String SERVICE_ID = "32251@3gpp.org";
    private static final long SUBSCRIBER_RANGE_START = 4474000000000L;
//    private static final long SUBSCRIBER_RANGE_START = 4604000000000L;
    private static final long SUBSCRIBER_COUNT = 1000L;
    private static BlockingQueue<Socket> SOCKETS;
    /////////////////////////////////////////////////////
    private final byte[] BYTE_ARR_BUFFER = new byte[4096];
    private final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(4096);
    private final Random random;
    private final byte[][] diameterMessages;
    private int requestNumber = 0;

    public static void main(String[] args) throws InterruptedException, IOException {
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        int tps = Integer.parseInt(args[2]);
        int duration = args.length > 3  ? Integer.parseInt(args[3]) : 600;
        int connectionCount = (tps / 4) + 1;
        SOCKETS = new ArrayBlockingQueue<>(connectionCount);
        ExecutorService service = Executors.newFixedThreadPool(tps == 0 ? 1 : (tps / 4));
        LOGGER.info("Creating tasks...");
        List<Callable<Void>> tasks = IntStream.range(0,
                                                     connectionCount == 1
                                                     ? 1
                                                     : (duration * tps))
                .parallel()
                .mapToObj(i -> new DiameterClient())
                .collect(Collectors.toList());
        LOGGER.info("Starting connections...");
        SOCKETS.addAll(IntStream.range(0, connectionCount)
                               .parallel()
                               .mapToObj(i -> initializeSocket(ip, port))
                               .toList());
        LOGGER.info("Connections are ready, starting load...");
        service.invokeAll(tasks);
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
        service.close();
        LOGGER.info("Load finished...");
        for (int i = 0; i < connectionCount; i++) {
            Socket socket = SOCKETS.take();
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
        }
    }

    public DiameterClient() {
        String session = "session-" + UUID.randomUUID();
        random = new Random();
        String msisdn = Long.toString(SUBSCRIBER_RANGE_START + random.nextLong(SUBSCRIBER_COUNT) + 1);
        diameterMessages = new byte[][]{ccrI(session, msisdn).convertToBytes(BYTE_BUFFER),
                                        ccrU(session, msisdn, getChargeValue()).convertToBytes(BYTE_BUFFER),
                                        ccrU(session, msisdn, getChargeValue()).convertToBytes(BYTE_BUFFER),
                                        ccrT(session, msisdn, getChargeValue()).convertToBytes(BYTE_BUFFER)};

    }

    @Override
    public Void call() {
        Socket ref = null;
        try {
            Socket socket;
            ref = socket = SOCKETS.take();
            long start = System.currentTimeMillis();
            sendMsgAndWaitForAnswer(socket, diameterMessages[0], "CCR-i");
            sleep(250 - System.currentTimeMillis() + start);
            start = System.currentTimeMillis();
            sendMsgAndWaitForAnswer(socket, diameterMessages[1], "CCR-u");
            sleep(250 - System.currentTimeMillis() + start);
            start = System.currentTimeMillis();
            sendMsgAndWaitForAnswer(socket, diameterMessages[2], "CCR-u");
            sleep(250 - System.currentTimeMillis() + start);
            start = System.currentTimeMillis();
            sendMsgAndWaitForAnswer(socket, diameterMessages[3], "CCR-t");
            sleep(250 - System.currentTimeMillis() + start);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to complete flow", e);
        } finally {
            if (ref != null) {
                SOCKETS.add(ref);
            }
        }
        return null;
    }

    private void sleep(long l) throws InterruptedException {
        Thread.sleep(l <= 0
                     ? 10
                     : l);
    }

    private int getChargeValue() {
        return (random.nextInt(50) + 1) * 1024 * 1024;
    }

    private void sendMsgAndWaitForAnswer(Socket socket, byte[] req, String msgName) {
        try {
            socket.getOutputStream().write(req);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(msgName + " sent... ");
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
                    if (LOGGER.isLoggable(Level.FINE)) {
                        String sessionId = dm.getAvp(SESSION_ID).getValue();
                        LOGGER.fine("Answer received. SessionId: |" + sessionId);
                        LOGGER.fine("Answer received. ResultCode: |" + resultCode);
                    }
                    if (resultCode != 2001) {
                        String error = dm.getAvp(AvpCodeTable.RFC.ERROR_MESSAGE).getValue();
                        LOGGER.severe("Error(" + resultCode + ") | " + error);
                    }
                    return;
                } else {
                    throw new RuntimeException("Failure - unexpected: " + dm);
                }
            }
            throw new RuntimeException("Problem on received answer");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Socket initializeSocket(String ip, int port) {
        Socket socket;
        do {
            try {
                socket = new Socket(ip, port);
                socket.setSoTimeout(100000);
                socket.setSoLinger(false, 0);
                cex(socket);
            } catch (Exception ignored) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                continue;
            }
            break;
        } while (true);
        return socket;
    }

    private static void cex(Socket socket) throws IOException {
        DiameterMessageHeader header = new DiameterMessageHeader.Builder(DiameterCommandCode.CE).setApplicationId(0)
                .setRequest()
                .setHopByHopId(0xa69025ddL)
                .setEndToEndId(0xb4b6e14cL)
                .build();

        List<Avp> avps = Arrays.asList(ORIGIN_REALM.createAvp("siemens.de"),
                                       ORIGIN_HOST.createAvp("bln1.siemens.de"),
                                       HOST_IP_ADDRESS.createAvp(Inet4Address.getLocalHost()),
                                       PRODUCT_NAME.createAvp("test"),
                                       VENDOR_ID.createAvp(1));

        DiameterMessage dm = new DiameterMessage(header, avps);
        byte[] BYTE_ARR_BUFFER = new byte[4096];
        ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(4096);
        socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER));
        int len = socket.getInputStream().read(BYTE_ARR_BUFFER);
        if (len > 19) {
            DiameterMessage da = new DiameterMessage(BYTE_ARR_BUFFER, len);
            Integer resultCode = da.getAvp(AvpCodeTable.RFC.RESULT_CODE).getValue();
            if (resultCode != 2001) {
                throw new RuntimeException("Failure: " + da);
            }
        } else {
            throw new RuntimeException("Problem on received answer");
        }
    }

    public DiameterMessage ccrI(String sessionId, String msisdn) {
        List<Avp> avps = List.of(SESSION_ID.createAvp(sessionId),
                                 ORIGIN_HOST.createAvp("diameterclient"),
                                 ORIGIN_REALM.createAvp("opencloud"),
                                 DESTINATION_HOST.createAvp("DestinationHost"),
                                 DESTINATION_REALM.createAvp("opencloud"),
                                 EVENT_TIMESTAMP.createAvp(ZonedDateTime.now()),
                                 AUTH_APPLICATION_ID.createAvp(4),
                                 SERVICE_CONTEXT_ID.createAvp(SERVICE_ID),
                                 CC_REQUEST_TYPE.createAvp(1),
                                 CC_REQUEST_NUMBER.createAvp(requestNumber++),
                                 SUBSCRIPTION_ID.createAvp(Map.of(SUBSCRIPTION_ID_TYPE,
                                                                  SUBSCRIPTION_ID_TYPE.createAvp(0),
                                                                  SUBSCRIPTION_ID_DATA,
                                                                  SUBSCRIPTION_ID_DATA.createAvp(msisdn))),
                                 SERVICE_INFORMATION_AVP,
                                 MULTIPLE_SERVICES_INDICATOR.createAvp(1),
                                 MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(Map.of(REQUESTED_SERVICE_UNIT,
                                                                                   REQUESTED_SERVICE_UNIT.createAvp(),
                                                                                   RATING_GROUP,
                                                                                   RATING_GROUP.createAvp(16))));
        DiameterMessageHeader header = new DiameterMessageHeader.Builder(DiameterCommandCode.CC).setApplicationId(4)
                .setEndToEndId(0x87b09775L)
                .setHopByHopId(0x00001c20)
                .setRequest()
                .setVersion((byte) 1)
                .build();
        return new DiameterMessage(header, avps);
    }

    public DiameterMessage ccrU(String sessionId, String msisdn, long chargeValue) {
        long inputValue = (long) (chargeValue * 0.1);
        long outputValue = chargeValue - inputValue;
        DiameterMessageHeader header = new DiameterMessageHeader.Builder(DiameterCommandCode.CC).setApplicationId(4)
                .setEndToEndId(0x87b09775L)
                .setHopByHopId(0x00001c20)
                .setRequest()
                .setVersion((byte) 1)
                .build();
        List<Avp> avps = List.of(SESSION_ID.createAvp(sessionId),
                                 ORIGIN_HOST.createAvp("diameterclient"),
                                 ORIGIN_REALM.createAvp("opencloud"),
                                 DESTINATION_HOST.createAvp("DestinationHost"),
                                 DESTINATION_REALM.createAvp("opencloud"),
                                 EVENT_TIMESTAMP.createAvp(ZonedDateTime.now()),
                                 AUTH_APPLICATION_ID.createAvp(4),
                                 SERVICE_CONTEXT_ID.createAvp(SERVICE_ID),
                                 CC_REQUEST_TYPE.createAvp(2),
                                 CC_REQUEST_NUMBER.createAvp(requestNumber++),
                                 SUBSCRIPTION_ID.createAvp(Map.of(SUBSCRIPTION_ID_TYPE,
                                                                  SUBSCRIPTION_ID_TYPE.createAvp(0),
                                                                  SUBSCRIPTION_ID_DATA,
                                                                  SUBSCRIPTION_ID_DATA.createAvp(msisdn))),
                                 MULTIPLE_SERVICES_INDICATOR.createAvp(1),
                                 SERVICE_INFORMATION_AVP,
                                 MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(Map.of(RATING_GROUP,
                                                                                   RATING_GROUP.createAvp(16),
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
        return new DiameterMessage(header, avps);
    }

    public DiameterMessage ccrT(String sessionId, String msisdn, long chargeValue) {
        long inputValue = (long) (chargeValue * 0.1);
        long outputValue = chargeValue - inputValue;
        DiameterMessageHeader header = new DiameterMessageHeader.Builder(DiameterCommandCode.CC).setApplicationId(4)
                .setEndToEndId(0x87b09775)
                .setHopByHopId(0x00001c20)
                .setRequest()
                .setVersion((byte) 1)
                .build();
        List<Avp> avps = List.of(SESSION_ID.createAvp(sessionId),
                                 ORIGIN_HOST.createAvp("diameterclient"),
                                 ORIGIN_REALM.createAvp("opencloud"),
                                 DESTINATION_HOST.createAvp("DestinationHost"),
                                 DESTINATION_REALM.createAvp("opencloud"),
                                 EVENT_TIMESTAMP.createAvp(ZonedDateTime.now()),
                                 AUTH_APPLICATION_ID.createAvp(4),
                                 SERVICE_CONTEXT_ID.createAvp(SERVICE_ID),
                                 CC_REQUEST_TYPE.createAvp(3),
                                 CC_REQUEST_NUMBER.createAvp(requestNumber++),
                                 SUBSCRIPTION_ID.createAvp(Map.of(SUBSCRIPTION_ID_TYPE,
                                                                  SUBSCRIPTION_ID_TYPE.createAvp(0),
                                                                  SUBSCRIPTION_ID_DATA,
                                                                  SUBSCRIPTION_ID_DATA.createAvp(msisdn))),
                                 MULTIPLE_SERVICES_INDICATOR.createAvp(1),
                                 SERVICE_INFORMATION_AVP,
                                 MULTIPLE_SERVICES_CREDIT_CONTROL.createAvp(Map.of(RATING_GROUP,
                                                                                   RATING_GROUP.createAvp(16),
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
        return new DiameterMessage(header, avps);
    }

    public static final Avp SERVICE_INFORMATION_AVP = SERVICE_INFORMATION.createAvp(Map.of(PS_INFORMATION,
                                                                                           PS_INFORMATION.createAvp(Map.of(
                                                                                                   CALLED_STATION_ID,
                                                                                                   CALLED_STATION_ID.createAvp(
                                                                                                           "iot.truphone.com"),
                                                                                                   TGPP_USER_LOCATION_INFO,
                                                                                                   TGPP_USER_LOCATION_INFO.createAvp(
                                                                                                           parseHexBinary("0162f2102f4c6bb6"))))));

    private static final Logger LOGGER = Logger.getLogger("DiameterClient");
}
