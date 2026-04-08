import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
/**
 * LogEvent represents one entry in the event log.
 * <p>
 * Each event stores:
 * - time: timestamp when event was created
 * - entity: component generating the event (Producer, Consumer, Buffer)
 * - code: event type (Add, Remove, WAIT_FULL, etc.)
 * - data: optional additional information
 */
public class LogEvent {
    final LocalDateTime time;
    final String entity;
    final String code;
    final String[] data;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    LogEvent(LocalDateTime time, String entity, String code, String... data) {
        this.time   = time;
        this.entity = entity;
        this.code   = code;
        this.data   = data;
    }

    String format() {
        StringBuilder sb = new StringBuilder("Log Event: [");
        sb.append(time.format(FORMATTER)).append(", ")
                .append(entity).append(", ")
                .append(code);
        for (String d : data) sb.append(", ").append(d);
        sb.append("]");
        return sb.toString();
    }
}
