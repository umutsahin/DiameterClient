package com.optiva.console;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Console {

    private static LineReader reader;

    static {
        //configureLogging();
        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder().provider("jni").nativeSignals(false).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        reader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    public static void log(String message) {
        reader.printAbove(message);
    }

    public static void error(String message) {
        AttributedString attrMessage = new AttributedStringBuilder().style(AttributedStyle.BOLD.foreground(
                AttributedStyle.RED)).append(message).style(AttributedStyle.DEFAULT).toAttributedString();
        reader.printAbove(attrMessage);
    }

    public static String readLine(String prompt) {
        try {
            return reader.readLine(prompt);
        } catch (Exception ignored) {
            // ignored
        }
        return null;
    }

    public static void configureLogging() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);

        Logger logger = Logger.getLogger("org.jline");
        logger.setLevel(Level.FINE);
        logger.addHandler(handler);
    }
}
