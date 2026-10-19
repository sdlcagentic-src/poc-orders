package orders;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Json {

    private static final Pattern ORDER_ID = Pattern.compile("\"orderId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ALL_AVAILABLE = Pattern.compile("\"allAvailable\"\\s*:\\s*(true|false)");
    private static final Pattern UNAVAILABLE = Pattern.compile("\"unavailableProductIds\"\\s*:\\s*\\[([^\\]]*)]");
    private static final Pattern STRING = Pattern.compile("\"([^\"]+)\"");

    record StockChecked(String orderId, boolean allAvailable, List<String> unavailableProductIds) {
    }

    private Json() {
    }

    static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    static String itemsArray(List<LineItem> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            LineItem item = items.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"productId\":").append(quote(item.productId().toString()))
                    .append(",\"quantity\":").append(item.quantity()).append('}');
        }
        return sb.append(']').toString();
    }

    static String idArray(List<String> ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(ids.get(i)));
        }
        return sb.append(']').toString();
    }

    static StockChecked parseStockChecked(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Empty stock check message");
        }
        Matcher order = ORDER_ID.matcher(payload);
        Matcher all = ALL_AVAILABLE.matcher(payload);
        if (!order.find() || !all.find()) {
            throw new IllegalArgumentException("Malformed stock check message");
        }
        List<String> unavailable = new ArrayList<>();
        Matcher list = UNAVAILABLE.matcher(payload);
        if (list.find()) {
            Matcher ids = STRING.matcher(list.group(1));
            while (ids.find()) {
                unavailable.add(ids.group(1));
            }
        }
        return new StockChecked(order.group(1), Boolean.parseBoolean(all.group(1)), unavailable);
    }
}
