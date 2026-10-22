package dev.ghost.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("ghost");
    private static final Path FILE = DIR.resolve("settings.json");

    private SettingsStore() {}

    public static GhostConfig load() {
        try {
            Files.createDirectories(DIR);
            if (Files.exists(FILE)) {
                GhostConfig config = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), GhostConfig.class);
                if (config != null) { config.normalize(); return config; }
            }
        } catch (Exception ignored) {
            // Configurações inválidas são ignoradas sem registrar conteúdo possivelmente sensível.
        }
        return new GhostConfig();
    }

    public static void save(GhostConfig config) throws IOException {
        Files.createDirectories(DIR);
        config.normalize();
        Files.writeString(FILE, GSON.toJson(config), StandardCharsets.UTF_8);
    }

    public static Path directory() { return DIR; }
}
