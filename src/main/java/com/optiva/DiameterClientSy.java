package com.optiva;

import com.optiva.diameter.DiameterAvp;
import com.optiva.diameter.DiameterAvpCodes;
import com.optiva.diameter.DiameterCommandCode;
import com.optiva.diameter.DiameterGroupedAvp;
import com.optiva.diameter.DiameterMessage;
import com.optiva.diameter.DiameterMessageHeader;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class DiameterClientSy {
    private static final byte[] BYTE_ARR_BUFFER = new byte[4096];
    private static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(4096);
    private static final Logger LOGGER = Logger.getLogger("DiameterClientSy");
    private static final CharsetDecoder DECODER = StandardCharsets.UTF_8.newDecoder();
    private static final CharsetEncoder ENCODER = StandardCharsets.UTF_8.newEncoder();
    private static final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private static final String sessionId = Long.toUnsignedString(new Random().nextLong());

    public static final long HOP_BY_HOP_ID = 0x00000001L;
    public static final long END_TO_END_ID = 0xb6e31ca7L;
    public static final long SY_APP = 16777302L;


    private static final DiameterMessage SLR;
    private static final DiameterMessage STR;

    public static void main(String[] args) {
        try (Socket socket = new Socket(args[0], Integer.parseInt(args[1]))) {
            socket.setSoTimeout(60000);
            socket.setSoLinger(false, 0);
            cex(socket);
            sleep(3000);
            sendRequestAndWaitForAnswer(socket, SLR, "SLR");
            sleep(1000);
            Thread thread = new Thread(() -> {
                try {
                    waitForNotification(socket);
                } catch (RuntimeException e) {
                    LOGGER.log(Level.SEVERE, "Unable to complete flow", e);
                } catch (Exception e) {
                    // ignored
                }
            });
            thread.start();
            Scanner sc = new Scanner(System.in);
            sc.next();
            thread.interrupt();
            clearOldSession(socket);
            socket.shutdownInput();
            socket.shutdownOutput();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    private static void cex(Socket socket) throws IOException {
        DiameterMessageHeader header = new DiameterMessageHeader.Builder(DiameterCommandCode.CE).setApplicationId(0)
                .setHopByHopId(HOP_BY_HOP_ID)
                .setEndToEndId(END_TO_END_ID)
                .build();

        List<DiameterAvp> avps =
                Arrays.asList(new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_REALM).setValue("siemens.de").build(),
                              new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_HOST).setValue("bln1.siemens.de")
                                      .build());

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

    private static void clearOldSession(Socket socket) throws IOException {
        try {
            sendRequestAndWaitForAnswer(socket, STR, "STR");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Session terminate not successful", e);
        }
    }

    private static void sendRequestAndWaitForAnswer(Socket socket,
                                                    DiameterMessage dm,
                                                    String msgName) throws IOException {
        socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER, ENCODER));
        LOGGER.info(msgName + " sent... ");
        int len;
        while ((len = socket.getInputStream().read(BYTE_ARR_BUFFER)) > 19) {
            dm = new DiameterMessage(BYTE_ARR_BUFFER, len, DECODER);
            if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.DW)) {
                dm.getHeader().setRequest(false);
                socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER, ENCODER));
            } else if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.SL) ||
                       dm.getHeader().getCommandCode().equals(DiameterCommandCode.ST)) {
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

    private static void waitForNotification(Socket socket) throws IOException {
        DiameterMessage dm;
        int len;
        while ((len = socket.getInputStream().read(BYTE_ARR_BUFFER)) > 19) {
            dm = new DiameterMessage(BYTE_ARR_BUFFER, len, DECODER);
            if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.DW)) {
                dm.getHeader().setRequest(false);
                socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER, ENCODER));
                if (!keepRunning.get()) {
                    break;
                }
            } else if (dm.getHeader().getCommandCode().equals(DiameterCommandCode.SN)) {
                LOGGER.info("SSNR received: " + dm);
                DiameterMessageHeader header = dm.getHeader();
                header.setRequest(false);
                List<DiameterAvp> avps = new ArrayList<>();
                avps.add(AVP_ORIGIN_HOST);
                avps.add(AVP_ORIGIN_REALM);
                avps.add(AVP_RESULT_CODE_2001);
                if (!dm.getAvp(DiameterAvpCodes.RFC.SESSION_ID).isEmpty()) {
                    avps.add(dm.getAvp(DiameterAvpCodes.RFC.SESSION_ID));
                }
                if (!dm.getAvp(DiameterAvpCodes.RFC.PROXY_INFO).isEmpty()) {
                    avps.add(dm.getAvp(DiameterAvpCodes.RFC.PROXY_INFO));
                }
                dm = new DiameterMessage(header, avps);
                LOGGER.info("Sending SSNA");
                socket.getOutputStream().write(dm.convertToBytes(BYTE_BUFFER, ENCODER));
            } else {
                throw new RuntimeException("Failure - unexpected: " + dm);
            }
        }
    }

    private static final DiameterAvp AVP_RESULT_CODE_2001 =
            new DiameterAvp.Builder(DiameterAvpCodes.RFC.RESULT_CODE).setMandatory().setValue(2001).build();

    public static final DiameterAvp AVP_SESSION_ID = new DiameterAvp.Builder(DiameterAvpCodes.RFC.SESSION_ID).setMandatory()
            .setValue("diacl_2;"+sessionId+";0")
            .build();

    public static final DiameterAvp AVP_AUTH_APPLICATION_ID =
            new DiameterAvp.Builder(DiameterAvpCodes.RFC.AUTH_APPLICATION_ID).setMandatory().setValue(16777302).build();

    public static final DiameterAvp AVP_ORIGIN_HOST =
            new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_HOST).setMandatory().setValue("diacl").build();

    public static final DiameterAvp AVP_ORIGIN_REALM = new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_REALM).setMandatory()
            .setValue("pcs.redknee.com")
            .build();

    public static final DiameterAvp AVP_DESTINATION_REALM =
            new DiameterAvp.Builder(DiameterAvpCodes.RFC.DESTINATION_REALM).setMandatory()
                    .setValue("unified_spr.redknee.com")
                    .build();

    public static final DiameterAvp AVP_SL_REQUEST_TYPE =
            new DiameterAvp.Builder(DiameterAvpCodes.TGPP.SL_REQUEST_TYPE).setMandatory()
                    .setVendor(10415)
                    .setValue(0)
                    .build();

    public static final DiameterAvp AVP_SUBSCRIPTION_ID_TYPE =
            new DiameterAvp.Builder(DiameterAvpCodes.RFC.SUBSCRIPTION_ID_TYPE).setMandatory().setValue(0).build();

    public static final DiameterAvp AVP_SUBSCRIPTION_ID_DATA =
            new DiameterAvp.Builder(DiameterAvpCodes.RFC.SUBSCRIPTION_ID_DATA).setMandatory()
                    .setValue("491713001129700")
                    .build();

    public static final DiameterAvp AVP_SUBSCRIPTION_ID =
            new DiameterAvp.Builder(DiameterAvpCodes.RFC.SUBSCRIPTION_ID).setMandatory()
                    .setValue(new DiameterGroupedAvp(AVP_SUBSCRIPTION_ID_TYPE, AVP_SUBSCRIPTION_ID_DATA))
                    .build();

    public static final DiameterAvp AVP_TERMINATION_CAUSE =
            new DiameterAvp.Builder(DiameterAvpCodes.RFC.TERMINATION_CAUSE).setMandatory().setValue(1).build();

    public static final DiameterMessageHeader HEADER_SL =
            new DiameterMessageHeader.Builder(DiameterCommandCode.SL)
                    .setRequest()
                    .setProxyable()
                    .setApplicationId(SY_APP)
                    .setHopByHopId(HOP_BY_HOP_ID)
                    .setEndToEndId(END_TO_END_ID)
                    .build();

    public static final DiameterMessageHeader HEADER_ST =
            new DiameterMessageHeader.Builder(DiameterCommandCode.ST)
                    .setRequest()
                    .setProxyable()
                    .setApplicationId(SY_APP)
                    .setHopByHopId(0x80L)
                    .setEndToEndId(0xb25a6a4eL)
                    .build();

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %6$s%n");
        List<DiameterAvp> avps = Arrays.asList(AVP_SESSION_ID,
                                               AVP_AUTH_APPLICATION_ID,
                                               AVP_ORIGIN_HOST,
                                               AVP_ORIGIN_REALM,
                                               AVP_DESTINATION_REALM,
                                               AVP_SL_REQUEST_TYPE,
                                               AVP_SUBSCRIPTION_ID);
        SLR = new DiameterMessage(HEADER_SL, avps);
        List<DiameterAvp> strAvps = Arrays.asList(AVP_SESSION_ID,
                                                  AVP_AUTH_APPLICATION_ID,
                                                  AVP_ORIGIN_HOST,
                                                  AVP_ORIGIN_REALM,
                                                  AVP_DESTINATION_REALM,
                                                  AVP_TERMINATION_CAUSE);
        STR = new DiameterMessage(HEADER_ST, strAvps);
    }
}
