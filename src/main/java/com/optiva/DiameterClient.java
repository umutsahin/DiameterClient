package com.optiva;

import com.optiva.diameter.DiameterAvp;
import com.optiva.diameter.DiameterAvpCodes;
import com.optiva.diameter.DiameterCommandCode;
import com.optiva.diameter.DiameterMessage;
import com.optiva.diameter.DiameterMessageHeader;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

public class DiameterClient implements Callable<Void>{
    private static final Logger LOGGER = Logger.getLogger("DiameterClientOMNT");
    private static final String CCR_I =
            "010003c4c000011000000004a69025ddb4b6e14c000001074000001a646961636c3b333833323338343939383b300000000001084000000d646961636c0000000000012840000017626c6e312e7369656d656e732e6465000000011b40000017626c6e312e7369656d656e732e646500000001024000000c00000004000001cd40000018362e333232353140336770702e6f7267000001a04000000c000000010000019f4000000c0000000000000001400000213936383133323339303939393035406f70746976612e636f6d000000000001164000000c41416e00000000374000000ce77a79cb000001bb40000028000001c24000000c00000000000001bc40000013393638373132313731363200000001bb4000002c000001c24000000c00000001000001bc4000001834323230323936383731323137313632000001c74000000c00000001000001ca00000024000001cb4000000c00000000000001cc4000000e494d45495356000000000369c0000144000028af0000036ac0000138000028af00000002c0000010000028afcd10e00f00000003c0000010000028af00000000000004cbc0000012000028af00010ab4a01b000000000005c0000023000028af30382d343430363030303138364130303030313836413000000004ccc0000012000028af0001c00a886f00000000034fc0000012000028af00017b7b7b40000000000008c0000012000028af343232303032000000000009c0000012000028af34323230303200000000000ac000000d000028af350000000000001e4000000c746169660000000cc000000d000028af300000000000000dc0000010000028af3034303000000012c0000012000028af3432323230300000000003ecc000000e000028af3232000000000016c0000014000028af0124220000fb56f600000015c000000d000028af0600000000000100c000001000003165000000000000011a4000000a323200000000011a4000000a323200000000011a400000306970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c0000011c400000bc00000118400000466970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c2e61726d2e70726f78792e7265646b6e65652e636f6d0000000000214000006a0100000000040000000000000000003331302e3132392e322e31393a333836383c3c2d2d31302e3133302e302e313a36353630265456212d4449414d455445522d30360005646961636c01000000010000003501000000010000006e0100000000000000";
    private static final String CCR_U =
            "010003c0c00001100000000470c20f04b4bcb64e000001074000001a646961636c3b333833323338343939383b300000000001084000000d646961636c0000000000012840000017626c6e312e7369656d656e732e6465000000011b40000017626c6e312e7369656d656e732e646500000001024000000c00000004000001cd40000018362e333232353140336770702e6f7267000001a04000000c000000020000019f4000000c00000001000001254000001872656473636c6470303033622e6f637300000001400000213936383133323339303939393035406f70746976612e636f6d000000000001164000000c41416e00000000374000000ce77a79cb000001bb40000028000001c24000000c00000000000001bc40000013393638373132313731363200000001bb4000002c000001c24000000c00000001000001bc4000001834323230323936383731323137313632000001c74000000c00000001000001c84000001c000001b540000008000001b04000000c00000063000001ca00000024000001cb4000000c00000000000001cc4000000e494d45495356000000000369c0000134000028af0000036ac0000128000028af00000002c0000010000028afcd10e00f00000003c0000010000028af00000000000004cbc0000012000028af00010ab4a01b000000000005c0000023000028af30382d343430363030303138364130303030313836413000000004ccc0000012000028af0001c00a886f00000000034fc0000012000028af00017b7b7b40000000000008c0000012000028af343232303032000000000009c0000012000028af34323230303200000000000ac000000d000028af350000000000001e4000000c746169660000000cc000000d000028af300000000000000dc0000010000028af3034303000000012c0000012000028af343232323030000000000016c0000014000028af012422002f4c6bb600000015c000000d000028af060000000000011a400000306970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c0000011c400000bc00000118400000466970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c2e61726d2e70726f78792e7265646b6e65652e636f6d0000000000214000006a0100000000040000000000000000003331302e3132392e322e31393a333836383c3c2d2d31302e3133302e302e313a36353630265456212d4449414d455445522d30360005646961636c01000000010000003501000000010000006e0100000000000000";
    private static final String CCR_T =
            "01000400c00001100000000449fce41db4b87a1c000001074000001a646961636c3b333833323338343939383b300000000001084000000d646961636c0000000000012840000017626c6e312e7369656d656e732e6465000000011b40000017626c6e312e7369656d656e732e646500000001024000000c00000004000001cd40000018362e333232353140336770702e6f7267000001a04000000c000000030000019f4000000c00000002000001254000001872656473636c6470303033622e6f637300000001400000213936383133323339303939393035406f70746976612e636f6d000000000001164000000c41416e00000000374000000ce77a79cb000001bb40000028000001c24000000c00000000000001bc40000013393638373132313731363200000001bb4000002c000001c24000000c00000001000001bc4000001834323230323936383731323137313632000001c74000000c00000001000001c84000005c000001be40000038000001a54000001000000000003200000000019c4000001000000000001900000000019e40000010000000000019000000000368c0000010000028af00000002000001b04000000c00000063000001ca00000024000001cb4000000c00000000000001cc4000000e494d45495356000000000369c0000134000028af0000036ac0000128000028af00000002c0000010000028afcd10e00f00000003c0000010000028af00000000000004cbc0000012000028af00010ab4a01b000000000005c0000023000028af30382d343430363030303138364130303030313836413000000004ccc0000012000028af0001c00a886f00000000034fc0000012000028af00017b7b7b40000000000008c0000012000028af343232303032000000000009c0000012000028af34323230303200000000000ac000000d000028af350000000000001e4000000c746169660000000cc000000d000028af300000000000000dc0000010000028af3034303000000012c0000012000028af343232303032000000000016c0000014000028af012422002f4c6bb600000015c000000d000028af060000000000011a400000306970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c0000011c400000bc00000118400000466970642d61696f2d302e6970642e6f636538333230342e7376632e636c75737465722e6c6f63616c2e61726d2e70726f78792e7265646b6e65652e636f6d0000000000214000006a0100000000040000000000000000003331302e3132392e322e31393a333836383c3c2d2d31302e3133302e302e313a36353630265456212d4449414d455445522d30360005646961636c01000000010000003501000000010000006e0100000000000000";

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %6$s%n");
    }

    private final String address;
    private final int port;
    private final byte[] BYTE_ARR_BUFFER = new byte[4096];
    private final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(4096);
    private final CharsetDecoder DECODER = StandardCharsets.UTF_8.newDecoder();
    private final CharsetEncoder ENCODER = StandardCharsets.UTF_8.newEncoder();
    private final String session;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(50);
        List<Callable<Void>> tasks = IntStream.range(0, Integer.parseInt(args[2]))
                .mapToObj(i -> new DiameterClient(args[0], args[1]))
                .collect(Collectors.toList());
        service.invokeAll(tasks);
        service.shutdown();
        service.awaitTermination(10, TimeUnit.MINUTES);
    }

    public DiameterClient(String address, String port) {
        this.address = address;
        this.port = Integer.parseInt(port);
        this.session = UUID.randomUUID().toString();
    }

    @Override
    public Void call() throws Exception {
        try {
            Socket socket = new Socket(address, port);
            socket.setSoTimeout(10000);
            socket.setSoLinger(false, 0);
            cex(socket);
            sleep(1000);
            sendMsgAndWaitForAnswer(socket, CCR_I, "CCR-i");
            sleep(1000);
            sendMsgAndWaitForAnswer(socket, CCR_U, "CCR-u");
            sleep(1000);
            sendMsgAndWaitForAnswer(socket, CCR_T, "CCR-t");
            LOGGER.info("Closing connection: " + socket.getRemoteSocketAddress());
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to complete flow", e);
        }
        return null;
    }

    private void sendMsgAndWaitForAnswer(Socket socket, String msg, String msgName) throws IOException {
        byte[] data = DatatypeConverter.parseHexBinary(msg);
        DiameterMessage dm = new DiameterMessage(data, DECODER);
        dm.replaceFirstAvp(DiameterAvpCodes.RFC.SESSION_ID, session);
        socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER, ENCODER));
        LOGGER.info(msgName + " sent... ");
        int len;
        while ((len = socket.getInputStream().read(BYTE_ARR_BUFFER)) > 19) {
            dm = new DiameterMessage(BYTE_ARR_BUFFER, len, DECODER);
            if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.DW)) {
                dm.getHeader().setRequest(false);
                socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER, ENCODER));
            } else if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.CC)) {
                String sessionId = dm.getAvp(DiameterAvpCodes.RFC.SESSION_ID).getDataAsUTF8String();
                Integer resultCode = dm.getAvp(DiameterAvpCodes.RFC.RESULT_CODE).getDataAsInteger32();
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

        List<DiameterAvp> avps =
                Arrays.asList(new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_REALM).setValue("siemens.de").build(),
                        new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_HOST).setValue("bln1.siemens.de").build(),
                        new DiameterAvp.Builder(DiameterAvpCodes.RFC.HOST_IP_ADDRESS).setValue(new byte[]{1, 1, 1, 1}).build(),
                        new DiameterAvp.Builder(DiameterAvpCodes.RFC.VENDOR_ID).setValue(1).build(),
                        new DiameterAvp.Builder(DiameterAvpCodes.RFC.PRODUCT_NAME).setValue("test").build()
                );

        DiameterMessage dm = new DiameterMessage(header, avps);
        socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER, ENCODER));
        LOGGER.info("CER sent... ");
        int len = socket.getInputStream().read(BYTE_ARR_BUFFER);
        if (len > 19) {
            DiameterMessage da = new DiameterMessage(BYTE_ARR_BUFFER, len, DECODER);
            Integer resultCode = da.getAvp(DiameterAvpCodes.RFC.RESULT_CODE).getDataAsInteger32();
            LOGGER.info("Answer received. ResultCode: |" + resultCode);
            if (resultCode != 2001) {
                throw new RuntimeException("Failure: " + da);
            }
        } else {
            throw new RuntimeException("Problem on received answer");
        }
    }
}
