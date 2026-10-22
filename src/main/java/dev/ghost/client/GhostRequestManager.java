package dev.ghost.client;

import dev.ghost.GhostClient;
import dev.ghost.ai.AiException;
import dev.ghost.ai.AiProvider;
import dev.ghost.ai.AiReply;
import dev.ghost.config.GhostConfig;
import dev.ghost.context.ClientContext;
import dev.ghost.history.ConversationHistory;
import dev.ghost.security.SecretStore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class GhostRequestManager {
    private final AiProvider provider;
    private final AtomicLong worldToken = new AtomicLong();
    private volatile ThreadPoolExecutor executor = newExecutor();
    private volatile Object lastWorld;

    public GhostRequestManager(AiProvider provider) { this.provider = provider; }

    public void onWorldChanged(Object world) {
        if (lastWorld == world) return;
        lastWorld = world;
        worldToken.incrementAndGet();
        ThreadPoolExecutor old = executor;
        executor = newExecutor();
        old.shutdownNow();
    }

    public boolean submit(String question, GhostConfig config) {
        String key = SecretStore.get();
        if (key.isBlank()) return false;
        long token = worldToken.get();
        String context = config.useGameContext ? ClientContext.collect(Minecraft.getInstance(), config) : "";
        String history = config.sessionHistory ? ConversationHistory.asText() : "";
        ThreadPoolExecutor pool = executor;
        try {
            pool.execute(() -> {
                if (token != worldToken.get()) return;
                try {
                    AiReply reply = provider.ask(key, config, question, context, history);
                    if (token != worldToken.get()) return;
                    GhostClient.setConnectionStatus("ghost.status.connected");
                    if (config.sessionHistory) ConversationHistory.add(question, reply.text());
                    String answer = reply.text();
                    if (!reply.sources().isEmpty()) answer += "\nFontes: " + String.join(" · ", reply.sources());
                    deliver(token, answer);
                } catch (AiException ex) {
                    if (ex.kind() != AiException.Kind.CANCELLED && token == worldToken.get()) {
                        GhostClient.setConnectionStatus(statusKey(ex.kind()));
                        deliverError(token, ex.kind());
                    }
                } catch (Throwable ignored) {
                    if (token == worldToken.get()) deliver(token, "Erro inesperado ao consultar a API.");
                }
            });
            return true;
        } catch (java.util.concurrent.RejectedExecutionException ex) { return false; }
    }

    public void testConnection(String key, String model, java.util.function.Consumer<String> callback) {
        if (key.isBlank()) { callback.accept("Chave não configurada."); return; }
        ThreadPoolExecutor pool = executor;
        try {
            pool.execute(() -> {
                String result;
                try { provider.testConnection(key, model); result = "ghost.status.connected"; }
                catch (AiException ex) { result = statusKey(ex.kind()); }
                GhostClient.setConnectionStatus(result);
                String finalResult = result;
                Minecraft.getInstance().execute(() -> callback.accept(Component.translatable(finalResult).getString()));
            });
        } catch (java.util.concurrent.RejectedExecutionException ex) { callback.accept("Fila ocupada; tente novamente."); }
    }

    private void deliver(long token, String text) {
        Minecraft.getInstance().execute(() -> {
            if (token != worldToken.get()) return;
            for (String part : split(text, 240)) Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("[Ghost] " + part));
        });
    }

    private void deliverError(long token, AiException.Kind kind) {
        Minecraft.getInstance().execute(() -> {
            if (token == worldToken.get()) Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.translatable(errorKey(kind)));
        });
    }

    private static String statusKey(AiException.Kind kind) { return switch (kind) {
        case INVALID_KEY -> "ghost.status.invalid_key";
        case MODEL -> "ghost.status.invalid_model";
        case QUOTA -> "ghost.status.quota";
        case RATE_LIMIT -> "ghost.status.rate_limit";
        case TIMEOUT -> "ghost.status.timeout";
        case OFFLINE -> "ghost.status.offline";
        case FORMAT -> "ghost.status.format";
        case CANCELLED -> "ghost.status.cancelled";
        case API -> "ghost.status.api_error";
    }; }

    private static String errorKey(AiException.Kind kind) { return switch (kind) {
        case INVALID_KEY -> "ghost.error.invalid_key";
        case MODEL -> "ghost.error.invalid_model";
        case QUOTA -> "ghost.error.quota";
        case RATE_LIMIT -> "ghost.error.api_rate_limit";
        case TIMEOUT -> "ghost.error.timeout";
        case OFFLINE -> "ghost.error.offline";
        case FORMAT -> "ghost.error.format";
        case CANCELLED -> "ghost.error.cancelled";
        case API -> "ghost.error.api";
    }; }

    public static List<String> split(String text, int max) {
        String normalized = text.replace('\r', ' ').replace('\n', ' ').trim();
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        while (normalized.length() > max) {
            int cut = normalized.lastIndexOf(' ', max);
            if (cut < max / 2) cut = max;
            parts.add(normalized.substring(0, cut).trim());
            normalized = normalized.substring(cut).trim();
        }
        if (!normalized.isEmpty()) parts.add(normalized);
        return parts;
    }

    private static ThreadPoolExecutor newExecutor() {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(2), runnable -> {
            Thread thread = new Thread(runnable, "Ghost-API"); thread.setDaemon(true); return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    public void close() { worldToken.incrementAndGet(); executor.shutdownNow(); }
}
