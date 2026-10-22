package dev.ghost.context;

public final class ContextSanitizer {
    private ContextSanitizer() {}

    public static String clean(String value, int maxChars) {
        if (value == null || maxChars <= 0) return "";
        String clean = value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").trim();
        if (clean.length() > maxChars) clean = clean.substring(0, maxChars);
        return clean;
    }
}
