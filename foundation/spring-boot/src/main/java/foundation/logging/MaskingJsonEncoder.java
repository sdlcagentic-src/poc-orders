package foundation.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.encoder.EncoderBase;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** One JSON object per line: time, level, logger, thread, message, the trace ids and the stack trace; message and stack trace are masked. */
public class MaskingJsonEncoder extends EncoderBase<ILoggingEvent> {

    private Masker masker = new Masker("email");

    public void setMasking(String mode) {
        this.masker = new Masker(mode.strip());
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder json = new StringBuilder(256).append('{');
        field(json, "timestamp", DateTimeFormatter.ISO_INSTANT.format(event.getInstant())).append(',');
        field(json, "level", event.getLevel().toString()).append(',');
        field(json, "logger", event.getLoggerName()).append(',');
        field(json, "thread", event.getThreadName()).append(',');
        field(json, "message", masker.mask(event.getFormattedMessage()));
        Map<String, String> mdc = event.getMDCPropertyMap();
        for (String key : new String[] {"traceId", "spanId"}) {
            if (mdc != null && mdc.get(key) != null) {
                json.append(',');
                field(json, key, mdc.get(key));
            }
        }
        if (event.getThrowableProxy() != null) {
            json.append(',');
            field(json, "stack_trace", masker.mask(ThrowableProxyUtil.asString(event.getThrowableProxy())));
        }
        return json.append("}\n").toString().getBytes(StandardCharsets.UTF_8);
    }

    private static StringBuilder field(StringBuilder json, String name, String value) {
        json.append('"').append(name).append("\":");
        if (value == null) {
            return json.append("null");
        }
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        return json.append('"');
    }
}
