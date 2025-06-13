package com.optiva.console;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Console {

    private static final AtomicLong LEVEL = new AtomicLong(0);
    private static final LineReader READER;
    private static final AttributedStyle ITALIC_MAGENTA = AttributedStyle.DEFAULT.italic()
            .foreground(AttributedStyle.MAGENTA);
    private static final AttributedStyle BOLD_YELLOW = AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle BOLD_RED = AttributedStyle.BOLD.foreground(AttributedStyle.RED);

    static {
        //configureLogging();
        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder().provider("jni").nativeSignals(false).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        READER = LineReaderBuilder.builder().terminal(terminal).build();
    }

    public static void log(String message) {
        READER.printAbove(message);
    }

    public static void debug(String message) {
        if (LEVEL.get() > 0) {
            AttributedString attrMessage = new AttributedStringBuilder().style(ITALIC_MAGENTA)
                    .append(message)
                    .style(AttributedStyle.DEFAULT)
                    .toAttributedString();
            READER.printAbove(attrMessage);
        }
    }

    public static void warn(String message) {
        AttributedString attrMessage = new AttributedStringBuilder().style(BOLD_YELLOW)
                .append(message)
                .style(AttributedStyle.DEFAULT)
                .toAttributedString();
        READER.printAbove(attrMessage);
    }

    public static void error(String message) {
        AttributedString attrMessage = new AttributedStringBuilder().style(BOLD_RED)
                .append(message)
                .style(AttributedStyle.DEFAULT)
                .toAttributedString();
        READER.printAbove(attrMessage);
    }

    public static String readLine(String prompt) {
        try {
            return READER.readLine(prompt);
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

    public static void setLevel(int level) {
        LEVEL.set(level);
    }
}
