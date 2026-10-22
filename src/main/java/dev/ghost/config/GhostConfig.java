package dev.ghost.config;

public final class GhostConfig {
    public String provider = "openai";
    public String model = "gpt-6-luna";
    public boolean useGameContext = true;
    public boolean sendExactXZ = false;
    public boolean extendedWorldContext = false;
    public boolean webSearch = false;
    public boolean sessionHistory = true;
    public boolean contextVersion = true;
    public boolean contextDimension = true;
    public boolean contextY = true;
    public boolean contextBiome = true;
    public boolean contextDifficulty = true;
    public boolean contextVitals = true;
    public boolean contextHeldItem = true;
    public boolean contextTarget = true;
    public int requestsPerMinute = 4;
    public int requestsPerHour = 30;

    public void normalize() {
        if (provider == null || !provider.equals("openai")) provider = "openai";
        if (model == null || model.isBlank()) model = "gpt-6-luna";
        model = model.trim().substring(0, Math.min(128, model.trim().length()));
        requestsPerMinute = Math.max(1, Math.min(30, requestsPerMinute));
        requestsPerHour = Math.max(requestsPerMinute, Math.min(240, requestsPerHour));
    }
}
