package dev.ghost.ui;

import dev.ghost.GhostClient;
import dev.ghost.config.GhostConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ContextSettingsScreen extends Screen {
    private final Screen parent;
    private final GhostConfig config;
    private final boolean[] values;
    private final String[] keys = {"version", "dimension", "y", "biome", "difficulty", "vitals", "held_item", "target"};
    private final boolean[] original;

    public ContextSettingsScreen(Screen parent) {
        super(Component.translatable("ghost.ui.context_title"));
        this.parent = parent;
        config = copy(GhostClient.config());
        values = read(config);
        original = values.clone();
    }

    @Override protected void init() {
        int left = width / 2 - 160;
        int y = Math.max(30, height / 2 - 150);
        for (int i = 0; i < keys.length; i++) {
            final int index = i;
            addRenderableWidget(Button.builder(label(index), button -> {
                values[index] = !values[index]; button.setMessage(label(index));
            }).bounds(left, y + i * 27, 320, 22).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("ghost.ui.save"), button -> {
            write(config, values); write(GhostClient.config(), values); Minecraft.getInstance().gui.setScreen(parent);
        }).bounds(left, y + 225, 154, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> {
            write(GhostClient.config(), original); Minecraft.getInstance().gui.setScreen(parent);
        }).bounds(left + 166, y + 225, 154, 22).build());
    }

    private Component label(int i) { return Component.translatable("ghost.ui.context." + keys[i]).append(Component.literal(": ")).append(Component.translatable(values[i] ? "ghost.common.on" : "ghost.common.off")); }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, Math.max(30, height / 2 - 170), 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("ghost.ui.context_warning"), width / 2, Math.max(30, height / 2 + 90), 0xE0C060);
    }

    @Override public void onClose() { write(GhostClient.config(), original); Minecraft.getInstance().gui.setScreen(parent); }

    private static GhostConfig copy(GhostConfig source) {
        GhostConfig c = new GhostConfig(); c.contextVersion=source.contextVersion; c.contextDimension=source.contextDimension; c.contextY=source.contextY;
        c.contextBiome=source.contextBiome; c.contextDifficulty=source.contextDifficulty; c.contextVitals=source.contextVitals;
        c.contextHeldItem=source.contextHeldItem; c.contextTarget=source.contextTarget; return c;
    }
    private static boolean[] read(GhostConfig c) { return new boolean[]{c.contextVersion,c.contextDimension,c.contextY,c.contextBiome,c.contextDifficulty,c.contextVitals,c.contextHeldItem,c.contextTarget}; }
    private static void write(GhostConfig c, boolean[] v) { c.contextVersion=v[0];c.contextDimension=v[1];c.contextY=v[2];c.contextBiome=v[3];c.contextDifficulty=v[4];c.contextVitals=v[5];c.contextHeldItem=v[6];c.contextTarget=v[7]; }
}
