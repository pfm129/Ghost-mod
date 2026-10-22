package dev.ghost;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ghost.ai.OpenAiProvider;
import dev.ghost.chat.GhostPrefix;
import dev.ghost.client.GhostRequestManager;
import dev.ghost.config.GhostConfig;
import dev.ghost.config.SettingsStore;
import dev.ghost.history.ConversationHistory;
import dev.ghost.limits.RequestLimiter;
import dev.ghost.security.SecretStore;
import dev.ghost.ui.GhostConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class GhostClient implements ClientModInitializer {
    private static GhostClient INSTANCE;
    private GhostConfig config;
    private final RequestLimiter limiter = new RequestLimiter();
    private GhostRequestManager requests;
    private KeyMapping openConfig;
    private Object previousWorld;
    private boolean noticeShown;
    private String connectionStatus = "ghost.status.not_tested";

    @Override public void onInitializeClient() {
        INSTANCE = this;
        config = SettingsStore.load();
        SecretStore.load();
        requests = new GhostRequestManager(new OpenAiProvider());
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ghost", "controls"));
        openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ghost.open_config", InputConstants.KEY_G, category));
        registerCommands();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> requests.close());
    }

    private void tick(Minecraft client) {
        while (openConfig.consumeClick()) client.gui.setScreen(new GhostConfigScreen(client.gui.screen()));
        Object world = client.level;
        if (world != previousWorld) {
            previousWorld = world;
            requests.onWorldChanged(world);
            noticeShown = false;
        }
        if (!SecretStore.isConfigured() && !noticeShown && client.player != null) {
            noticeShown = true;
            Component message = Component.translatable("ghost.notice.configure")
                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/ghost config")));
            client.execute(() -> client.gui.hud.getChat().addClientSystemMessage(message));
        }
    }

    /** Retorna true quando o texto foi reconhecido e deve ser cancelado no ChatScreen. */
    public static boolean handleChatInput(String rawMessage) {
        Optional<String> questionOption = GhostPrefix.extractQuestion(rawMessage);
        if (questionOption.isEmpty()) return false;
        Minecraft client = Minecraft.getInstance();
        String question = questionOption.get();
        if (question.isBlank()) {
            client.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.chat.prompt_hint"));
            return true;
        }
        if (question.length() > 1200) {
            client.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.error.question_too_long"));
            return true;
        }
        if (!SecretStore.isConfigured()) {
            client.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.notice.configure"));
            client.gui.setScreen(new GhostConfigScreen(client.gui.screen()));
            return true;
        }
        GhostClient self = INSTANCE;
        GhostConfig cfg = self.config;
        if (!self.limiter.allow(cfg.requestsPerMinute, cfg.requestsPerHour, System.currentTimeMillis())) {
            client.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.error.rate_local"));
            return true;
        }
        client.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.chat.thinking"));
        if (!self.requests.submit(question, cfg)) client.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.error.queue_full"));
        return true;
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("ghost")
                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("config").executes(ctx -> {
                            Minecraft.getInstance().gui.setScreen(new GhostConfigScreen(Minecraft.getInstance().gui.screen())); return 1;
                        }))
                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("clear").executes(ctx -> {
                            ConversationHistory.clear(); Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.status.history_cleared")); return 1;
                        }))
                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("status").executes(ctx -> {
                            showStatus(); return 1;
                        }))
                        .executes(ctx -> {
                            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.command.usage")); return 1;
                        })
        ));
    }

    private void showStatus() {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.status.line1", SecretStore.isConfigured() ? Component.translatable("ghost.common.yes") : Component.translatable("ghost.common.no")));
        mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.status.line2", config.provider, config.model));
        mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.status.line3", Component.translatable(connectionStatus)));
        mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.status.line4", config.useGameContext ? Component.translatable("ghost.common.on") : Component.translatable("ghost.common.off"), config.webSearch ? Component.translatable("ghost.common.on") : Component.translatable("ghost.common.off")));
        mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("ghost.status.line5", config.requestsPerMinute, config.requestsPerHour));
    }

    public static GhostConfig config() { return INSTANCE.config; }
    public static void saveConfig(GhostConfig config) throws java.io.IOException { config.normalize(); SettingsStore.save(config); INSTANCE.config = config; }
    public static GhostRequestManager requests() { return INSTANCE.requests; }
    public static void clearLocalHistory() { ConversationHistory.clear(); }
    public static void setConnectionStatus(String translationKey) { INSTANCE.connectionStatus = translationKey; }
    public static String connectionStatus() { return INSTANCE.connectionStatus; }
    public static void replaceConfigInMemory(GhostConfig config) { config.normalize(); INSTANCE.config = config; }
}
