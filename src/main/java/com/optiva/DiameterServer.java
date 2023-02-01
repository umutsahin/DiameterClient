package com.optiva;

import com.optiva.diameter.DiameterAvp;
import com.optiva.diameter.DiameterAvpCodes;
import com.optiva.diameter.DiameterCommandCode;
import com.optiva.diameter.DiameterMessage;
import com.optiva.diameter.DiameterMessageHeader;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DiameterServer {
    private static final CharsetDecoder DECODER = StandardCharsets.UTF_8.newDecoder();
    private static final CharsetEncoder ENCODER = StandardCharsets.UTF_8.newEncoder();
    private static boolean CE_MSG_REQUIRED = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(38680));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        ByteBuffer buffer = ByteBuffer.allocate(4096);

        while (true) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                System.out.println("--------------------------");
                if (key.isAcceptable()) {
                    accept(selector, serverSocket);
                } else if (key.isReadable()) {
                    read(selector, key, buffer);
                } else if (key.isWritable()) {
                    write(selector, key, buffer);
                }
            }
        }
    }

    private static void write(Selector selector, SelectionKey key, ByteBuffer buffer) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        DiameterMessage answer;
        if (key.attachment() != null && key.attachment() instanceof DiameterMessage) {
            answer = (DiameterMessage) key.attachment();
        } else {
            answer = DeviceWatchdog.DWR;
        }
        buffer.clear();
        answer.convertToByteBuffer(buffer, ENCODER);
        final int write = client.write(buffer);
        System.out.printf("Write successful(%d): %s\n", write, client.getRemoteAddress());
        System.out.println("Type: " + answer.getHeader().getCommandCode());
        client.register(selector, SelectionKey.OP_READ);
    }

    private static void read(Selector selector, SelectionKey key, ByteBuffer buffer) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        int read = 0;
        try {
            read = client.read(buffer);
        } catch (IOException e) {
            System.out.println("Client disconnected: " + client.getRemoteAddress() + "|" + e.getMessage());
            client.close();
            return;
        }
        System.out.println("Read successful(" + read + ") :" + client.getRemoteAddress());
        buffer.flip();
        if (buffer.hasRemaining()) {
            DiameterMessage diameterMessage = new DiameterMessage(buffer, DECODER);
            if (key.attachment() != null) {
                if (diameterMessage.getHeader().getCommandCode().equals(DiameterCommandCode.CE)) {
                    DiameterMessage answer = DiameterAnswer.createCE(diameterMessage);
                    client.register(selector, SelectionKey.OP_WRITE, answer);
                    System.out.println("Received CE: " + client.getRemoteAddress());
                } else {
                    System.out.println("Error: Diameter CE expected as first message");
                    client.close();
                }
            } else {
                System.out.println("Received Message: ");
                System.out.println("Type: " + diameterMessage.getHeader().getCommandCode());
                System.out.println("Session ID: " + diameterMessage.getAvp(DiameterAvpCodes.RFC.SESSION_ID).getDataAsUTF8String());
                String bytes = "010001e040000110000000046a0abb3d501ef43600000107400000746474642d302e7461732d6d746c30322e696d732e6d6e633030342e6d63633634332e336770706e6574776f726b2e6f72673b333833303331343232353b3338303b626569643a474953552d302e6d746c30327461732e6c6f63616c3b31363436343b323832383b3338303b30000001284000000f6e736e2e636f6d000000010840000017756e69666965642e6e736e2e636f6d00000001a04000000c000000010000019f4000000c00000000000001024000000c00000004000001c8400000340000010c4000000c000007d1000001af40000014000001a44000000c0000012c000001c04000000c000002580000010c4000000c000007d10000011c400000cc00000118400000296164636c61622d697064312e61726d2e70726f78792e7265646b6e65652e636f6d00000000000021400000980100000000040000000000000000003531302e3230312e392e323a333836383c3c2d2d31302e3132332e35312e3139373a33383638265456212d4449414d455445522d303600316474642d302e7461732d6d746c30322e696d732e6d6e633030342e6d63633634332e336770706e6574776f726b2e6f7267010000000f000000be0100000001000000c8010000000000";
                final byte[] ccAnswerBytes = DatatypeConverter.parseHexBinary(bytes);
                DiameterMessage answer = new DiameterMessage(ccAnswerBytes, DECODER);
                answer.getHeader().setHopByHopId(diameterMessage.getHeader().getHopByHopId());
                answer.getHeader().setEndToEndId(diameterMessage.getHeader().getEndToEndId());
                client.register(selector, SelectionKey.OP_WRITE, answer);
            }
        } else {
            client.register(selector, SelectionKey.OP_WRITE);
        }
    }

    private static void accept(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, Boolean.TRUE);
        System.out.println("Client connected: " + client.getRemoteAddress());
    }

    private static class DeviceWatchdog {
        public static DiameterMessage DWR;

        static {
            DiameterMessageHeader header = new DiameterMessageHeader.Builder(DiameterCommandCode.DW)
                    .setApplicationId(0)
                    .setHopByHopId(Long.parseLong("6c3d8958", 16))
                    .setEndToEndId(Long.parseLong("6c3d8958", 16))
                    .setRequest()
                    .build();

            List<DiameterAvp> avps = Arrays.asList(
                    new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_HOST)
                            .setValue("origin-host")
                            .build(),
                    new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_REALM)
                            .setValue("origin-realm")
                            .build()
            );

            DWR = new DiameterMessage(header, avps);
        }
    }

    private static class DiameterAnswer {
        public static DiameterMessage create(DiameterMessage dm) {
            DiameterMessageHeader header = new DiameterMessageHeader.Builder(DiameterCommandCode.CC)
                    .setApplicationId(4)
                    .setHopByHopId(dm.getHeader().getHopByHopId())
                    .setEndToEndId(dm.getHeader().getEndToEndId())
                    .build();

            List<DiameterAvp> avps = Arrays.asList(
                    new DiameterAvp.Builder(DiameterAvpCodes.RFC.RESULT_CODE)
                            .setValue(2001)
                            .build(),
                    new DiameterAvp.Builder(DiameterAvpCodes.RFC.SESSION_ID)
                            .setValue(dm.getAvp(DiameterAvpCodes.RFC.SESSION_ID).getDataAsUTF8String())
                            .build()
            );

            return new DiameterMessage(header, avps);
        }

        public static DiameterMessage createCE(DiameterMessage dm) {
            DiameterMessageHeader header = new DiameterMessageHeader.Builder(DiameterCommandCode.CE)
                    .setApplicationId(0)
                    .setHopByHopId(dm.getHeader().getHopByHopId())
                    .setEndToEndId(dm.getHeader().getEndToEndId())
                    .build();

            List<DiameterAvp> avps = Arrays.asList(
                    new DiameterAvp.Builder(DiameterAvpCodes.RFC.RESULT_CODE)
                            .setValue(2001)
                            .build(),
                    new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_REALM)
                            .setValue("realm")
                            .build(),
                    new DiameterAvp.Builder(DiameterAvpCodes.RFC.ORIGIN_HOST)
                            .setValue("host")
                            .build()
            );

            return new DiameterMessage(header, avps);
        }
    }
}
