package foundation.logging;

import java.util.regex.Pattern;

/** Redacts personal data from log text: {@code none}, {@code email}, or {@code email-phone}. */
public final class Masker {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9][0-9 ()-]{7,}[0-9]");

    private final boolean email;
    private final boolean phone;

    public Masker(String mode) {
        switch (mode) {
            case "none" -> {
                email = false;
                phone = false;
            }
            case "email" -> {
                email = true;
                phone = false;
            }
            case "email-phone" -> {
                email = true;
                phone = true;
            }
            default -> throw new IllegalArgumentException("unknown masking mode: " + mode);
        }
    }

    public String mask(String text) {
        if (text == null) {
            return null;
        }
        String masked = email ? EMAIL.matcher(text).replaceAll("[email]") : text;
        return phone ? PHONE.matcher(masked).replaceAll("[phone]") : masked;
    }
}
