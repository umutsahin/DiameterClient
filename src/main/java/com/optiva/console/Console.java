package com.optiva.console;

import com.optiva.OpenTelemetryConfig;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Console {

    private static final AtomicLong LEVEL = new AtomicLong(0);
    private static final LineReader READER;
    private static final AttributedStyle ITALIC_MAGENTA = AttributedStyle.DEFAULT.italic()
            .foreground(AttributedStyle.MAGENTA);
    private static final AttributedStyle ITALIC_CYAN = AttributedStyle.DEFAULT.italic()
            .foreground(AttributedStyle.CYAN);
    private static final AttributedStyle BOLD_YELLOW = AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle BOLD_RED = AttributedStyle.BOLD.foreground(AttributedStyle.RED);
    private static final String NUMERIC = "[n]";
    private static final List<Command> COMMANDS = List.of(new Command("flow",
                                                                      List.of("ps", "ims-moc", "ims-mtc"),
                                                                      "flow type to run, default ims-moc"),
                                                          new Command("rating-group",
                                                                      List.of(NUMERIC),
                                                                      "rating group of messages to be sent, default 10"),
                                                          new Command("message-count",
                                                                      List.of(NUMERIC),
                                                                      "message count within single session, default 4"),
                                                          new Command("single", List.of(), "runs single session"),
                                                          new Command("rps", List.of(NUMERIC), "request per second"),
                                                          new Command("log-level", List.of("0", "1", "2"),
                                                                      "enable/disable debug logs"),
                                                          new Command("exit", List.of(), "graceful shutdown"));

    static {
        //configureLogging();
        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder().provider("jni").nativeSignals(false).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AggregateCompleter completer = new AggregateCompleter(COMMANDS.stream().map(c -> {
            if (c.parameters().isEmpty()) {
                return new StringsCompleter(c.command());
            } else if (c.parameters.size() == 1 && NUMERIC.equals(c.parameters().getFirst())) {
                return new StringsCompleter(c.command());
            } else {
                return new ArgumentCompleter(new StringsCompleter(c.command()), new StringsCompleter(c.parameters()));
            }
        }).toList());
        READER = LineReaderBuilder.builder().terminal(terminal).completer(completer).build();
    }

    public static void log(String message) {
        READER.printAbove(message);
    }

    public static void debug(String message) {
        OpenTelemetryConfig.otelLogger.logRecordBuilder()
                .setTimestamp(Instant.now())
                .setBody(message)
                .setSeverity(Severity.DEBUG)
                .setContext(Context.current())
                .emit();
        if (LEVEL.get() == 1) {
            AttributedString attrMessage = new AttributedStringBuilder().style(ITALIC_MAGENTA)
                    .append(message)
                    .style(AttributedStyle.DEFAULT)
                    .toAttributedString();
            READER.printAbove(attrMessage);
        }
    }

    public static void trace(String message) {
        OpenTelemetryConfig.otelLogger.logRecordBuilder()
                .setTimestamp(Instant.now())
                .setBody(message)
                .setSeverity(Severity.TRACE)
                .setContext(Context.current())
                .emit();
        if (LEVEL.get() == 2) {
            AttributedString attrMessage = new AttributedStringBuilder().style(ITALIC_CYAN)
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

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("org.jline");
        logger.setLevel(Level.FINE);
        logger.addHandler(handler);
    }

    public static void setLevel(int level) {
        LEVEL.set(level);
    }

    record Command(String command, List<String> parameters, String listItem, String description) {
        Command(String command, List<String> parameters, String description) {
            this(command,
                 parameters,
                 parameters.isEmpty()
                 ? command
                 : command + " " + calculateParameters(parameters),
                 description);
        }

        private static String calculateParameters(List<String> parameters) {
            if (parameters.size() == 1 && NUMERIC.equals(parameters.getFirst())) {
                return NUMERIC;
            }
            return "[" + String.join("|", parameters) + "]";
        }
    }

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
