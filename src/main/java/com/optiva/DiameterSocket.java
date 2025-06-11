package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.avp.AvpCodeTable;
import com.optiva.flows.DiameterCE;
import com.optiva.flows.DiameterFBC;
import com.optiva.flows.DiameterFlow;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DiameterSocket {
    private final ScheduledExecutorService eventLoop;
    private final ScheduledExecutorService status;
    private final ScheduledExecutorService scheduler;
    private final int sessionCount;
    private final int messagePerSession;
    private final int requestPerSecond;
    private final int totalDurationInSeconds;
    private final Queue<DiameterFlow> sessionQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, DiameterFlow> activeFlows = new ConcurrentHashMap<>();
    private final Selector selector;
    private final Random random = new Random();
    private final SocketChannel socketChannel;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong sentRequests = new AtomicLong();
    private final AtomicLong receivedAnswers = new AtomicLong();
    private final AtomicLong prevSentRequests = new AtomicLong(0);
    private final AtomicLong prevReceivedAnswers = new AtomicLong(0);
    private final int size;
    private boolean isCapabilitiesExchangeReceived = false;
    private final long rate;
    private final AtomicLong previousDelay = new AtomicLong(0);

    public DiameterSocket(int sessionCount,
                          int messagePerSession,
                          int requestPerSecond,
                          int totalDurationInSeconds,
                          int size) throws IOException {
        this.sessionCount = sessionCount;
        this.messagePerSession = messagePerSession;
        this.requestPerSecond = requestPerSecond;
        this.totalDurationInSeconds = totalDurationInSeconds;
        this.rate = (1000L * size / requestPerSecond);
        AtomicInteger counter = new AtomicInteger(0);
        this.size = size;
        eventLoop = Executors.newScheduledThreadPool(size,
                                                     r -> new Thread(r,
                                                                     "DiameterSocket-EventLoop-"
                                                                     + counter.incrementAndGet()));
        status = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Status"));
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "DiameterSocket-Scheduler"));
        selector = Selector.open();
        socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 3868));
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            activeFlows.clear();
            eventLoop.shutdownNow();
            scheduler.shutdownNow();
            status.shutdownNow();
            System.out.printf("""
                              Total Requests Sent: %d
                              Total Requests Received: %d
                              """, sentRequests.get(), receivedAnswers.get());
        }, "Shutdown-Thread"));
    }

    public void start() {
        handleCE();
        startLoad();
        handleDurationEnd();
        startStatus();
        handleFlow();
    }

    private void handleCE() {
        try {
            DiameterCE diameterCE = new DiameterCE();
            ByteBuffer ceMessage = diameterCE.getNextMessage();
            while (ceMessage.hasRemaining()) {
                socketChannel.write(ceMessage);
            }
            while (!isCapabilitiesExchangeReceived) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    if (key.isReadable()) {
                        ByteBuffer buffer = DiameterFlow.BUFFER.get();
                        buffer.clear();
                        int read = socketChannel.read(buffer);
                        if (read == -1) {
                            throw new RuntimeException("Error reading CEA from socket");
                        }
                        buffer.flip();
                        DiameterMessage cea = new DiameterMessage(buffer);
                        int resultCode = cea.getAvp(AvpCodeTable.RFC.RESULT_CODE).getValue();
                        if (resultCode != 2001) {
                            throw new RuntimeException("Failure on CE, " + cea);
                        } else {
                            isCapabilitiesExchangeReceived = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error sending CE: " + e.getMessage());
        }

    }

    private void startLoad() {
        System.out.printf("Starting load with %d TPS\n", requestPerSecond);
        while (activeFlows.size() < requestPerSecond) {
            String msisdn = Long.toString(4474000000000L + random.nextLong(sessionCount) + 1);
            DiameterFBC fbc = new DiameterFBC(msisdn, 16, messagePerSession);
            sessionQueue.add(fbc);
            activeFlows.put(fbc.getKey(), fbc);
        }
    }

    private void handleDurationEnd() {
        scheduler.schedule(() -> {
            System.out.println("Duration ended, waiting for sessions to complete...");
            running.set(false);
        }, totalDurationInSeconds, TimeUnit.SECONDS);
    }

    private void startStatus() {
        int period = 10;
        Instant startTime = Instant.now();
        status.scheduleAtFixedRate(() -> {
            long req = sentRequests.get();
            long pReq = prevSentRequests.get();
            long ans = receivedAnswers.get();
            long pAns = prevReceivedAnswers.get();
            double requestRate = ((double) (req - pReq)) / period;
            double answerRate = ((double) (ans - pAns)) / period;
            long duration = Duration.between(startTime, Instant.now()).getSeconds();
            System.out.printf("""
                              -----------------------------------
                              Current Send TPS:%-4.2f, Time:%03d/%03ds
                              Current Recv TPS:%-4.2f, Time:%03d/%03ds
                              -----------------------------------
                              """,
                              requestRate,
                              duration,
                              totalDurationInSeconds,
                              answerRate,
                              duration,
                              totalDurationInSeconds);
            prevSentRequests.set(req);
            prevReceivedAnswers.set(ans);
            if (!running.get()) {
                status.shutdown();
            }
        }, 0, period, TimeUnit.SECONDS);
    }

    private void handleFlow() {
        try {
            while (running.get() || !activeFlows.isEmpty()) {
                int readyCount = selector.selectNow();
                if (readyCount == 0) {
                    continue;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isWritable()) {
                        handleWritable(key);
                    }
                    if (key.isReadable()) {
                        handleReadable(key);
                    }
                }
            }
            System.out.println("All sessions completed...");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            status.shutdownNow();
            eventLoop.shutdownNow();
            scheduler.shutdownNow();
        }
    }

    @SuppressWarnings("resource")
    private void handleWritable(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        final DiameterFlow flow = sessionQueue.poll();
        if (flow == null) {
            return;
        }
        eventLoop.schedule(() -> {
            try {
                long start = System.currentTimeMillis();
                ByteBuffer buffer = flow.getNextMessage();
                if (buffer == null) {
                    if (running.get()) {
                        DiameterFlow newFlow = flow.restart();
                        buffer = newFlow.getNextMessage();
                        activeFlows.remove(flow.getKey());
                        activeFlows.put(newFlow.getKey(), newFlow);
                    } else {
                        activeFlows.remove(flow.getKey());
                        return;
                    }
                }
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                sentRequests.incrementAndGet();
                previousDelay.set(System.currentTimeMillis() - start);
            } catch (Exception e) {
                System.err.println("Error reading from server aaaa: " + e.getMessage());
            }
        }, rate - (previousDelay.get() * size), TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("resource")
    private void handleReadable(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        eventLoop.execute(() -> {
            try {
                ByteBuffer buffer = DiameterFlow.BUFFER.get();
                buffer.clear();
                int read = channel.read(buffer);
                if (read == -1) {
                    throw new RuntimeException("Failure reading from server");
                }
                if (read == 0) {
                    return;
                }
                buffer.flip();
                while (read != buffer.position()) {
                    DiameterMessage cca = new DiameterMessage(buffer);
                    receivedAnswers.incrementAndGet();
                    int resultCode = cca.getAvpValue(AvpCodeTable.RFC.RESULT_CODE);
                    String session = cca.getAvpValue(AvpCodeTable.RFC.SESSION_ID);
                    if (resultCode != 2001) {
                        String msg = cca.getAvpValue(AvpCodeTable.RFC.ERROR_MESSAGE);
                        System.err.println("Error(" + resultCode + ") | Session(" + session + ") : " + msg);
                        activeFlows.remove(session);
                        continue;
                    }
                    DiameterFlow flow = activeFlows.get(session);
                    sessionQueue.add(flow);
                    buffer.limit(read);
                }
            } catch (IOException e) {
                System.err.println("Error reading from server: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) throws IOException {
        DiameterSocket ds = new DiameterSocket(1, 5, 1, 1, 1);
        ds.start();
    }
}
