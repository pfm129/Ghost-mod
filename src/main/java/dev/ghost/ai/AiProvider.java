package dev.ghost.ai;

import dev.ghost.config.GhostConfig;

public interface AiProvider {
    AiReply ask(String apiKey, GhostConfig config, String question, String context, String sessionHistory) throws AiException;
    void testConnection(String apiKey, String model) throws AiException;
}
