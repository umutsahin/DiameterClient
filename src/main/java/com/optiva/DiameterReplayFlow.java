package com.optiva;

import com.optiva.charging.openapi.diameter.DiameterMessage;
import com.optiva.charging.openapi.diameter.common.DatatypeConverter;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.ERROR_MESSAGE;
import static com.optiva.charging.openapi.diameter.common.enumeration.AvpCodeTable.RFC.RESULT_CODE;

public class DiameterReplayFlow {
    private static final byte[] BYTE_ARR_BUFFER = new byte[4096];
    private static final Logger logger = Logger.getLogger(Thread.currentThread().getName());

    public static void main(String[] args) throws IOException {
        String filePath = args[0];
        List<String> lines = Files.readAllLines(Paths.get(filePath));

        try (Socket socket = initializeSocket("127.0.0.1", 3868)) {
            for (String line : lines) {
                byte[] bytes = DatatypeConverter.parseHexBinary(line);
                socket.getOutputStream().write(bytes);
                waitForResponse(socket);
            }
            logger.info("Completed flow");
        }
    }

    public static void waitForResponse(Socket socket) throws IOException {
        int len;
        DiameterMessage dm;
        while ((len = socket.getInputStream().read(BYTE_ARR_BUFFER)) > 19) {
            dm = new DiameterMessage(BYTE_ARR_BUFFER, len);
            Integer resultCode = dm.getAvp(RESULT_CODE).getValue();
            if (resultCode != 2001) {
                String error = dm.getAvp(ERROR_MESSAGE).getValue();
                logger.severe("Error(" + resultCode + ") | " + error);
                throw new RuntimeException(error);
            }
            return;
        }
    }

    public static Socket initializeSocket(String ip, int port) {
        Socket socket;
        do {
            try {
                socket = new Socket(ip, port);
                socket.setSoTimeout(1000000);
                socket.setSoLinger(false, 0);
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
        logger.info("Connected to " + ip + ":" + port);
        return socket;
    }
}
