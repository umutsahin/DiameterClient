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
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.HOST_IP_ADDRESS;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.ORIGIN_HOST;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.ORIGIN_REALM;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.PRODUCT_NAME;
import static com.optiva.charging.openapi.diameter.avp.AvpCodeTable.RFC.VENDOR_ID;

public class DiameterLoadRunner {
    public static final int CALL_SLEEP = 500;
    private static final Logger LOGGER = Logger.getLogger(DiameterLoadRunner.class.getSimpleName());
    private static String ip;
    private static int port;
    private final LinkedBlockingQueue<Socket> socketsQueue;
    private final AtomicLong counter;
    private ScheduledExecutorService service;
    private Instant startTime;

    public static void main(String[] args) throws InterruptedException {
        ip = args[0];
        port = Integer.parseInt(args[1]);
        int tps = Integer.parseInt(args[2]);
        int duration = Integer.parseInt(args[3]);
        long subscriberRangeStart = Long.parseLong(args[4]);
        int subscriberCount = Integer.parseInt(args[5]);
        new DiameterLoadRunner(tps, duration, subscriberRangeStart, subscriberCount);
    }

    public DiameterLoadRunner(int tps,
                              int duration,
                              long subscriberRangeStart,
                              int subscriberCount) throws InterruptedException {
        int cc = CALL_SLEEP * 4 / 1000;
        int threadCount = Math.max(tps * CALL_SLEEP / 1000, 1);
        int callCount = Math.ceilDiv(duration, cc);
        int createSleep = CALL_SLEEP / threadCount;
        duration = callCount * cc;
        LOGGER.info("Starting connections...");
        socketsQueue = prepareConnections(threadCount);
        counter = new AtomicLong(0);
        LOGGER.info("Starting load...");
        LOGGER.info("TPS: " + tps + ", Duration: " + duration + " seconds");
        if (tps == 1 && callCount == 1) {
            new DiameterClient(this, socketsQueue, counter, subscriberRangeStart, subscriberCount).run();
        } else {
            service = Executors.newScheduledThreadPool(threadCount + 1);
            startTime = Instant.now();
            List<? extends ScheduledFuture<?>> scheduledTasks = IntStream.range(0, threadCount).boxed().map(i -> {
                try {
                    Thread.sleep(i * createSleep);
                } catch (InterruptedException e) {
                    // ignored
                }
                return service.scheduleAtFixedRate(new DiameterClient(this,
                                                                      socketsQueue,
                                                                      counter,
                                                                      subscriberRangeStart,
                                                                      subscriberCount), 0, 1, TimeUnit.SECONDS);
            }).toList();
            service.schedule(() -> {
                scheduledTasks.forEach(s -> s.cancel(false));
                service.shutdown();
            }, duration, TimeUnit.SECONDS);
            logStatus(duration);
            LOGGER.info("Waiting for threads to stop...");
            boolean ignored = service.awaitTermination(10, TimeUnit.SECONDS);
            LOGGER.info("Closing connections...");
        }
        closeConnections();
        LOGGER.info("Total requests sent: " + counter.get() + ", Total duration: " + duration);
    }

    private void closeConnections() {
        socketsQueue.forEach(s -> {
            try {
                s.close();
            } catch (IOException e) {
                //ignored
            }
        });
    }

    private void logStatus(int duration) {
        long start = System.currentTimeMillis();
        long end;
        long prevCount = 0;
        long newCount;
        while (!service.isShutdown()) {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                //ignored
            }
            newCount = counter.get();
            end = System.currentTimeMillis();
            long rate = (newCount - prevCount) * 1000 / (end - start);
            LOGGER.info(MessageFormat.format("Current TPS: {0}, Time:{1}/{2}s",
                                             rate,
                                             Duration.between(startTime, Instant.now()).getSeconds(),
                                             duration));
            start = end;
            prevCount = newCount;
        }
    }

    private LinkedBlockingQueue<Socket> prepareConnections(int threadCount) {
        return IntStream.range(0, threadCount)
                .mapToObj(i -> initializeSocket())
                .collect(Collectors.toCollection(LinkedBlockingQueue::new));
    }

    public Socket initializeSocket() {
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
                    //ignored
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
}
