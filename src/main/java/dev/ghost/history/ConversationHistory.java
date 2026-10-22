package dev.ghost.history;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;

public final class ConversationHistory {
    private record Turn(String question, String answer) {}
    private static final Deque<Turn> TURNS = new ArrayDeque<>();
    private ConversationHistory() {}

    public static synchronized void add(String question, String answer) {
        TURNS.addLast(new Turn(trim(question, 500), trim(answer, 1200)));
        while (TURNS.size() > 8) TURNS.removeFirst();
    }

    public static synchronized String asText() {
        return TURNS.stream().map(turn -> "Jogador: " + turn.question + "\nGhost: " + turn.answer).collect(Collectors.joining("\n"));
    }

    public static synchronized void clear() { TURNS.clear(); }
    private static String trim(String text, int length) { return text.length() <= length ? text : text.substring(0, length); }
}
