package dev.ghost.ui;

import dev.ghost.GhostClient;
import dev.ghost.config.GhostConfig;
import dev.ghost.security.SecretStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GhostConfigScreen extends Screen {
    private final Screen parent;
    private EditBox secretField, modelField, rpmField, hourField;
    private boolean secretVisible;
    private boolean useContext, exactXZ, extended, webSearch, history;
    private String testStatus = "";
    private Button secretToggle;
    private final GhostConfig original;
    private boolean saved;

    public GhostConfigScreen(Screen parent) { super(Component.translatable("ghost.ui.title")); this.parent = parent; this.original = copy(GhostClient.config()); }

    @Override protected void init() {
        GhostConfig cfg = GhostClient.config();
        useContext = cfg.useGameContext; exactXZ = cfg.sendExactXZ; extended = cfg.extendedWorldContext;
        webSearch = cfg.webSearch; history = cfg.sessionHistory;
        int left = width / 2 - 210;
        int y = Math.max(32, height / 2 - 205);

        secretField = new EditBox(font, left, y, 335, 20, Component.translatable("ghost.ui.api_key"));
        secretField.setMaxLength(300);
        secretField.setValue(SecretStore.get());
        secretField.addFormatter((value, cursor) -> Component.literal(secretVisible ? value : "•".repeat(value.length())).getVisualOrderText());
        addRenderableWidget(secretField);
        secretToggle = Button.builder(toggleVisibilityLabel(), button -> {
            secretVisible = !secretVisible; secretToggle.setMessage(toggleVisibilityLabel());
        }).bounds(left + 341, y, 79, 20).build();
        addRenderableWidget(secretToggle);

        modelField = new EditBox(font, left, y + 29, 250, 20, Component.translatable("ghost.ui.model"));
        modelField.setMaxLength(128); modelField.setValue(cfg.model); addRenderableWidget(modelField);
        addRenderableWidget(Button.builder(Component.translatable("ghost.ui.test"), button -> testConnection())
                .bounds(left + 258, y + 29, 162, 20).build());

        addToggle(left, y + 58, "ghost.ui.game_context", () -> useContext, value -> useContext = value);
        addToggle(left, y + 84, "ghost.ui.exact_xz", () -> exactXZ, value -> exactXZ = value);
        addToggle(left, y + 110, "ghost.ui.extended", () -> extended, value -> extended = value);
        addToggle(left, y + 136, "ghost.ui.web_search", () -> webSearch, value -> webSearch = value);
        addToggle(left, y + 162, "ghost.ui.history", () -> history, value -> history = value);
        addRenderableWidget(Button.builder(Component.translatable("ghost.ui.context_categories"), button -> Minecraft.getInstance().gui.setScreen(new ContextSettingsScreen(this)))
                .bounds(left, y + 188, 420, 22).build());

        rpmField = new EditBox(font, left + 126, y + 220, 54, 20, Component.translatable("ghost.ui.rpm"));
        rpmField.setMaxLength(2); rpmField.setValue(Integer.toString(cfg.requestsPerMinute)); addRenderableWidget(rpmField);
        hourField = new EditBox(font, left + 305, y + 220, 54, 20, Component.translatable("ghost.ui.rph"));
        hourField.setMaxLength(3); hourField.setValue(Integer.toString(cfg.requestsPerHour)); addRenderableWidget(hourField);

        int buttonY = y + 257;
        addRenderableWidget(Button.builder(Component.translatable("ghost.ui.save"), button -> save())
                .bounds(left, buttonY, 204, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("ghost.ui.clear_key"), button -> {
            SecretStore.clear(); secretField.setValue(""); testStatus = getTranslated("ghost.ui.key_erased");
        }).bounds(left + 210, buttonY, 210, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("ghost.ui.clear_history"), button -> {
            GhostClient.clearLocalHistory(); testStatus = getTranslated("ghost.ui.history_cleared");
        }).bounds(left, buttonY + 27, 204, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 210, buttonY + 27, 210, 22).build());
    }

    private Button addToggle(int x, int y, String labelKey, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
        Button button = Button.builder(toggleLabel(labelKey, getter.get()), b -> {
            setter.accept(!getter.get()); b.setMessage(toggleLabel(labelKey, getter.get()));
        }).bounds(x, y, 420, 22).build();
        addRenderableWidget(button); return button;
    }

    private Component toggleLabel(String key, boolean enabled) {
        return Component.translatable(key).append(Component.literal(": ")).append(Component.translatable(enabled ? "ghost.common.on" : "ghost.common.off"));
    }

    private Component toggleVisibilityLabel() { return Component.translatable(secretVisible ? "ghost.ui.hide_key" : "ghost.ui.show_key"); }

    private void testConnection() {
        String model = modelField.getValue().trim();
        if (model.isBlank()) { testStatus = getTranslated("ghost.error.model_empty"); return; }
        testStatus = getTranslated("ghost.ui.testing");
        GhostClient.requests().testConnection(secretField.getValue(), model, result -> testStatus = result);
    }

    private void save() {
        GhostConfig cfg = GhostClient.config();
        cfg.model = modelField.getValue().trim();
        cfg.useGameContext = useContext; cfg.sendExactXZ = exactXZ; cfg.extendedWorldContext = extended;
        cfg.webSearch = webSearch; cfg.sessionHistory = history;
        cfg.requestsPerMinute = parseLimit(rpmField.getValue(), 4);
        cfg.requestsPerHour = parseLimit(hourField.getValue(), 30);
        cfg.normalize();
        String key = secretField.getValue().trim();
        if (key.isBlank() && !SecretStore.isConfigured()) { testStatus = getTranslated("ghost.error.key_required"); return; }
        try {
            if (!key.isBlank()) {
                boolean protectedOnDisk = SecretStore.activate(key);
                testStatus = getTranslated(protectedOnDisk ? "ghost.ui.saved" : "ghost.ui.session_only");
            } else testStatus = getTranslated("ghost.ui.saved");
            GhostClient.saveConfig(cfg); saved = true;
            if (SecretStore.isConfigured()) Minecraft.getInstance().gui.setScreen(parent);
        } catch (Exception ignored) { testStatus = getTranslated("ghost.error.save"); }
    }

    private int parseLimit(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (RuntimeException ex) { return fallback; }
    }

    private String getTranslated(String key) { return Component.translatable(key).getString(); }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int left = width / 2 - 210;
        int y = Math.max(32, height / 2 - 205);
        graphics.centeredText(font, title, width / 2, y - 19, 0xFFFFFF);
        graphics.text(font, Component.translatable("ghost.ui.api_key"), left, y - 11, 0xB0B0B0);
        graphics.text(font, Component.translatable("ghost.ui.model"), left, y + 18, 0xB0B0B0);
        graphics.text(font, Component.translatable("ghost.ui.rpm"), left, y + 226, 0xFFFFFF);
        graphics.text(font, Component.translatable("ghost.ui.rph"), left + 185, y + 226, 0xFFFFFF);
        graphics.text(font, Component.translatable("ghost.ui.privacy_note"), left, y + 248, 0xE0C060);
        if (extended) graphics.text(font, Component.translatable("ghost.ui.extended_warning"), left, y + 282, 0xE0C060);
        if (!testStatus.isBlank()) graphics.centeredText(font, testStatus, width / 2, y + 317, 0xFFFFFF);
    }

    @Override public void onClose() {
        if (!saved) GhostClient.replaceConfigInMemory(original);
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private static GhostConfig copy(GhostConfig s) {
        GhostConfig c = new GhostConfig(); c.provider=s.provider;c.model=s.model;c.useGameContext=s.useGameContext;c.sendExactXZ=s.sendExactXZ;
        c.extendedWorldContext=s.extendedWorldContext;c.webSearch=s.webSearch;c.sessionHistory=s.sessionHistory;
        c.contextVersion=s.contextVersion;c.contextDimension=s.contextDimension;c.contextY=s.contextY;c.contextBiome=s.contextBiome;
        c.contextDifficulty=s.contextDifficulty;c.contextVitals=s.contextVitals;c.contextHeldItem=s.contextHeldItem;c.contextTarget=s.contextTarget;
        c.requestsPerMinute=s.requestsPerMinute;c.requestsPerHour=s.requestsPerHour;return c;
    }
}
