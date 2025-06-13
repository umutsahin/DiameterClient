package com.optiva.console;

import com.optiva.OpenTelemetryConfig; // Add this
import io.opentelemetry.api.logs.Severity; // Add this
import io.opentelemetry.api.trace.Span; // Add this
import io.opentelemetry.context.Context; // Add this
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        StringsCompleter completer = new StringsCompleter("single", "rps", "debug", "exit");
        READER = LineReaderBuilder.builder().terminal(terminal).completer(completer).build();
    }

    public static void log(String message) {
        READER.printAbove(message);
    }

    public static void debug(String message) {
        if (LEVEL.get() > 0) {
            // Original JLine logging
            AttributedString attrMessage = new AttributedStringBuilder().style(ITALIC_MAGENTA)
                    .append(message)
                    .style(AttributedStyle.DEFAULT)
                    .toAttributedString();
            READER.printAbove(attrMessage);

            // OpenTelemetry Logging
            io.opentelemetry.api.logs.Logger otelLogger = OpenTelemetryConfig.getOtelLogger();
            if (otelLogger != null) {
                otelLogger.logRecordBuilder()
                    .setBody(message)
                    .setSeverity(Severity.DEBUG)
                    // Associate with current span if any
                    .setContext(Context.current()) // This carries the current SpanContext
                    .emit();
            }
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

    public static String readPrompt() {
        try {
            return READER.readLine(PROMPT);
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

    record Command(String command, List<String> parameters, String listItem, String description) {
        Command(String command, List<String> parameters, String description) {
            this(command, parameters, parameters.isEmpty() ? command : command + " " + String.join(" ", parameters), description);
        }
    }

    private static final List<Command> COMMANDS = List.of(new Command("single", List.of(), "runs single session"),
                                                          new Command("rps", List.of("[n]"), "request per second"),
                                                          new Command("debug",
                                                                      List.of("[0|1]"),
                                                                      "enable/disable debug logs"),
                                                          new Command("exit", List.of(), "graceful shutdown")
    );

    private static final String PROMPT_PREFIX = """
                                         ================================================================================
                                         Commands:
                                         """;

    private static final String PROMPT_SUFFIX = "command> ";

    public static String constructCommandsPrompt() {
        Integer maxLength = COMMANDS.stream()
                .map(c -> c.listItem().length())
                .max(Integer::compareTo)
                .orElseThrow(() -> new RuntimeException("No commands found"));
        return COMMANDS.stream()
                .map(c -> "* "
                          + c.listItem()
                          + " ".repeat(maxLength - c.listItem().length() + 1)
                          + ": "
                          + c.description()
                          + System.lineSeparator())
                .collect(Collectors.joining());

    }

    private static final String PROMPT = PROMPT_PREFIX + constructCommandsPrompt() + PROMPT_SUFFIX;
}
