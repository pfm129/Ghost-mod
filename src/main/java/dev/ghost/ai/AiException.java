package dev.ghost.ai;

public final class AiException extends Exception {
    public enum Kind { INVALID_KEY, MODEL, QUOTA, RATE_LIMIT, TIMEOUT, OFFLINE, FORMAT, API, CANCELLED }
    private final Kind kind;
    public AiException(Kind kind, String safeMessage) { super(safeMessage); this.kind = kind; }
    public Kind kind() { return kind; }
}
