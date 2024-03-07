package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterCommandCode;
import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.DiameterMessageHeader;
import com.optiva.charging.openapi.diameter.avp.Avp;
import com.optiva.charging.openapi.diameter.avp.AvpCodeTable;
import jakarta.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
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

public class DiameterClient implements Callable<Void> {
    public static final String SERVICE_ID = "32251@3gpp.org";
    private static final int TPS = 1000;
    private static int THR_COUNT = 20;
    private static BlockingQueue<Socket> SOCKETS;
    private final byte[] BYTE_ARR_BUFFER = new byte[4096];
    private final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(4096);
    private final String session;
    private final String msisdn;
    private final Random random;
    private final int updateCount;
    private int requestNumber = 0;

    private static void sleep(int nanos) throws InterruptedException {
        Thread.sleep(0, nanos);
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        THR_COUNT = Integer.parseInt(args[2]);
        SOCKETS = new ArrayBlockingQueue<>(THR_COUNT);
        ExecutorService service = Executors.newFixedThreadPool(THR_COUNT);
        List<Callable<Void>> tasks = IntStream.range(0, Integer.parseInt(args[3]))
                .mapToObj(i -> new DiameterClient())
                .collect(Collectors.toList());
        for (int i = 0; i < THR_COUNT; i++) {
            Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
            socket.setSoTimeout(10000);
            socket.setSoLinger(false, 0);
            SOCKETS.add(socket);
        }
        service.invokeAll(tasks);
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
        service.close();
        for (int i = 0; i < THR_COUNT; i++) {
            Socket socket = SOCKETS.take();
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
        }
    }

    public DiameterClient() {
        this.session = "session-" + UUID.randomUUID();
        random = new Random();
        this.updateCount = random.nextInt(10) + 1;
        this.msisdn = Long.toString(4474000000000L + random.nextLong(THR_COUNT) + 1);
    }

    @Override
    public Void call() {
        int msgCount = 1 + 1 + updateCount + 1;
        int sleep = 1000000 / TPS / msgCount;
        Socket socket;
        try {
            socket = SOCKETS.take();
        } catch (InterruptedException e) {
            return null;
        }
        try {
            cex(socket);
            sleep(sleep);
            sendMsgAndWaitForAnswer(socket, ccrI(session, msisdn), "CCR-i");
            for (int i = 0; i < updateCount; i++) {
                sleep(sleep);
                sendMsgAndWaitForAnswer(socket, ccrU(session, msisdn, getChargeValue()), "CCR-u");
            }
            sleep(sleep);
            sendMsgAndWaitForAnswer(socket, ccrT(session, msisdn, getChargeValue()), "CCR-t");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to complete flow", e);
        } finally {
            SOCKETS.add(socket);
        }
        return null;
    }

    private int getChargeValue() {
        return (random.nextInt(50) + 1) * 1024 * 1024;
    }

    private void sendMsgAndWaitForAnswer(Socket socket, DiameterMessage dm, String msgName) throws IOException {
        socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER));
        LOGGER.info(msgName + " sent... ");
        int len;
        while ((len = socket.getInputStream().read(BYTE_ARR_BUFFER)) > 19) {
            dm = new DiameterMessage(BYTE_ARR_BUFFER, len);
            if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.DW)) {
                dm.getHeader().setRequest(false);
                socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER));
            } else if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.CC)) {
                String sessionId = dm.getAvp(SESSION_ID).getValue();
                Integer resultCode = dm.getAvp(AvpCodeTable.RFC.RESULT_CODE).getValue();
                LOGGER.info("Answer received. SessionId: |" + sessionId);
                LOGGER.info("Answer received. ResultCode: |" + resultCode);
                if (resultCode != 2001) {
                    throw new RuntimeException("Failure: " + dm);
                } else {
                    return;
                }
            } else {
                throw new RuntimeException("Failure - unexpected: " + dm);
            }
        }
        throw new RuntimeException("Problem on received answer");
    }

    private void cex(Socket socket) throws IOException {
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
        socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER));
        LOGGER.info("CER sent... ");
        int len = socket.getInputStream().read(BYTE_ARR_BUFFER);
        if (len > 19) {
            DiameterMessage da = new DiameterMessage(BYTE_ARR_BUFFER, len);
            Integer resultCode = da.getAvp(AvpCodeTable.RFC.RESULT_CODE).getValue();
            LOGGER.info("Answer received. ResultCode: |" + resultCode);
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
                                                                                   RATING_GROUP.createAvp(1))));
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
                                                                                   RATING_GROUP.createAvp(1),
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
                                                                                   RATING_GROUP.createAvp(1),
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
                                                                                                           "simulator"),
                                                                                                   TGPP_USER_LOCATION_INFO,
                                                                                                   TGPP_USER_LOCATION_INFO.createAvp(
                                                                                                           Base64.getDecoder()
                                                                                                                   .decode("0107f41000fb56f6"))))));

    private static final Logger LOGGER = Logger.getLogger("DiameterClient");
    private static final DiameterMessage CCR_I;
    private static final DiameterMessage CCR_U;
    private static final DiameterMessage CCR_T;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %6$s%n");
        //        LOGGER.setLevel(Level.OFF);
        CCR_I = new DiameterMessage(DatatypeConverter.parseHexBinary(
                "010003c4c000011000000004a69025ddb4b6e14c000001074000001a646961636c3b333833323338343939383b300000000001084000000d646961636c0000000000012840000017626c6e312e7369656d656e732e6465000000011b40000017626c6e312e7369656d656e732e646500000001024000000c00000004000001cd40000018362e333232353140336770702e6f7267000001a04000000c000000010000019f4000000c0000000000000001400000213936383133323339303939393035406f70746976612e636f6d000000000001164000000c41416e00000000374000000ce77a79cb000001bb40000028000001c24000000c00000000000001bc40000013393638373132313731363200000001bb4000002c000001c24000000c00000001000001bc4000001834323230323936383731323137313632000001c74000000c00000001000001ca00000024000001cb4000000c00000000000001cc4000000e494d45495356000000000369c0000144000028af0000036ac0000138000028af00000002c0000010000028afcd10e00f00000003c0000010000028af00000000000004cbc0000012000028af00010ab4a01b000000000005c0000023000028af30382d343430363030303138364130303030313836413000000004ccc0000012000028af0001c00a886f00000000034fc0000012000028af00017b7b7b40000000000008c0000012000028af343232303032000000000009c0000012000028af34323230303200000000000ac000000d000028af350000000000001e4000000c746169660000000cc000000d000028af300000000000000dc0000010000028af3034303000000012c0000012000028af3432323230300000000003ecc000000e000028af3232000000000016c0000014000028af0124220000fb56f600000015c000000d000028af0600000000000100c000001000003165000000000000011a4000000a323200000000011a4000000a323200000000011a400000306970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c0000011c400000bc00000118400000466970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c2e61726d2e70726f78792e7265646b6e65652e636f6d0000000000214000006a0100000000040000000000000000003331302e3132392e322e31393a333836383c3c2d2d31302e3133302e302e313a36353630265456212d4449414d455445522d30360005646961636c01000000010000003501000000010000006e0100000000000000"));
        CCR_U = new DiameterMessage(DatatypeConverter.parseHexBinary(
                "010003c0c00001100000000470c20f04b4bcb64e000001074000001a646961636c3b333833323338343939383b300000000001084000000d646961636c0000000000012840000017626c6e312e7369656d656e732e6465000000011b40000017626c6e312e7369656d656e732e646500000001024000000c00000004000001cd40000018362e333232353140336770702e6f7267000001a04000000c000000020000019f4000000c00000001000001254000001872656473636c6470303033622e6f637300000001400000213936383133323339303939393035406f70746976612e636f6d000000000001164000000c41416e00000000374000000ce77a79cb000001bb40000028000001c24000000c00000000000001bc40000013393638373132313731363200000001bb4000002c000001c24000000c00000001000001bc4000001834323230323936383731323137313632000001c74000000c00000001000001c84000001c000001b540000008000001b04000000c00000063000001ca00000024000001cb4000000c00000000000001cc4000000e494d45495356000000000369c0000134000028af0000036ac0000128000028af00000002c0000010000028afcd10e00f00000003c0000010000028af00000000000004cbc0000012000028af00010ab4a01b000000000005c0000023000028af30382d343430363030303138364130303030313836413000000004ccc0000012000028af0001c00a886f00000000034fc0000012000028af00017b7b7b40000000000008c0000012000028af343232303032000000000009c0000012000028af34323230303200000000000ac000000d000028af350000000000001e4000000c746169660000000cc000000d000028af300000000000000dc0000010000028af3034303000000012c0000012000028af343232323030000000000016c0000014000028af012422002f4c6bb600000015c000000d000028af060000000000011a400000306970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c0000011c400000bc00000118400000466970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c2e61726d2e70726f78792e7265646b6e65652e636f6d0000000000214000006a0100000000040000000000000000003331302e3132392e322e31393a333836383c3c2d2d31302e3133302e302e313a36353630265456212d4449414d455445522d30360005646961636c01000000010000003501000000010000006e0100000000000000"));
        CCR_T = new DiameterMessage(DatatypeConverter.parseHexBinary(
                "01000400c00001100000000449fce41db4b87a1c000001074000001a646961636c3b333833323338343939383b300000000001084000000d646961636c0000000000012840000017626c6e312e7369656d656e732e6465000000011b40000017626c6e312e7369656d656e732e646500000001024000000c00000004000001cd40000018362e333232353140336770702e6f7267000001a04000000c000000030000019f4000000c00000002000001254000001872656473636c6470303033622e6f637300000001400000213936383133323339303939393035406f70746976612e636f6d000000000001164000000c41416e00000000374000000ce77a79cb000001bb40000028000001c24000000c00000000000001bc40000013393638373132313731363200000001bb4000002c000001c24000000c00000001000001bc4000001834323230323936383731323137313632000001c74000000c00000001000001c84000005c000001be40000038000001a54000001000000000003200000000019c4000001000000000001900000000019e40000010000000000019000000000368c0000010000028af00000002000001b04000000c00000063000001ca00000024000001cb4000000c00000000000001cc4000000e494d45495356000000000369c0000134000028af0000036ac0000128000028af00000002c0000010000028afcd10e00f00000003c0000010000028af00000000000004cbc0000012000028af00010ab4a01b000000000005c0000023000028af30382d343430363030303138364130303030313836413000000004ccc0000012000028af0001c00a886f00000000034fc0000012000028af00017b7b7b40000000000008c0000012000028af343232303032000000000009c0000012000028af34323230303200000000000ac000000d000028af350000000000001e4000000c746169660000000cc000000d000028af300000000000000dc0000010000028af3034303000000012c0000012000028af343232303032000000000016c0000014000028af012422002f4c6bb600000015c000000d000028af060000000000011a400000306970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c0000011c400000bc00000118400000466970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c2e61726d2e70726f78792e7265646b6e65652e636f6d0000000000214000006a0100000000040000000000000000003331302e3132392e322e31393a333836383c3c2d2d31302e3133302e302e313a36353630265456212d4449414d455445522d30360005646961636c01000000010000003501000000010000006e0100000000000000"));
    }
}
