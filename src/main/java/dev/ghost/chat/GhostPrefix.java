package dev.ghost.chat;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GhostPrefix {
    private static final Pattern PREFIX = Pattern.compile("(?i)^ghost(?=$|[\\s,:])(?:[\\s,:]+)?(.*)$", Pattern.DOTALL);
    private GhostPrefix() {}

    public static Optional<String> extractQuestion(String message) {
        if (message == null) return Optional.empty();
        Matcher matcher = PREFIX.matcher(message.stripLeading());
        return matcher.matches() ? Optional.of(matcher.group(1).trim()) : Optional.empty();
    }

    public static boolean isGhostMessage(String message) { return extractQuestion(message).isPresent(); }
}
