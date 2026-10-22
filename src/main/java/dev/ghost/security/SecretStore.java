package dev.ghost.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.platform.win32.Crypt32Util;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/** DPAPI (CurrentUser) no Windows; fora dele a chave permanece somente na memória. */
public final class SecretStore {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ghost").resolve("secret.json");
    private static volatile String sessionKey = "";
    private static volatile boolean persistentProtectionAvailable;

    private SecretStore() {}

    public static synchronized void load() {
        sessionKey = "";
        persistentProtectionAvailable = false;
        if (!isWindows() || !Files.exists(FILE)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            byte[] encrypted = Base64.getDecoder().decode(json.get("dpapi").getAsString());
            sessionKey = new String(Crypt32Util.cryptUnprotectData(encrypted), StandardCharsets.UTF_8);
            persistentProtectionAvailable = !sessionKey.isBlank();
        } catch (Throwable ignored) {
            sessionKey = "";
            persistentProtectionAvailable = false;
        }
    }

    /** @return true se a chave foi persistida sob DPAPI; false significa somente sessão. */
    public static synchronized boolean activate(String key) {
        sessionKey = key == null ? "" : key.trim();
        persistentProtectionAvailable = false;
        if (sessionKey.isEmpty() || !isWindows()) return false;
        try {
            Files.createDirectories(FILE.getParent());
            byte[] protectedBytes = Crypt32Util.cryptProtectData(sessionKey.getBytes(StandardCharsets.UTF_8));
            JsonObject json = new JsonObject();
            json.addProperty("dpapi", Base64.getEncoder().encodeToString(protectedBytes));
            Files.writeString(FILE, json.toString(), StandardCharsets.UTF_8);
            persistentProtectionAvailable = true;
            return true;
        } catch (Throwable ignored) {
            try { Files.deleteIfExists(FILE); } catch (Exception ignoredDelete) {}
            return false;
        }
    }

    public static synchronized void clear() {
        sessionKey = "";
        persistentProtectionAvailable = false;
        try { Files.deleteIfExists(FILE); } catch (Exception ignored) {}
    }

    public static String get() { return sessionKey; }
    public static boolean isConfigured() { return !sessionKey.isBlank(); }
    public static boolean isPersistentProtectionAvailable() { return persistentProtectionAvailable; }
    private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }
}
