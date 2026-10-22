package dev.ghost.context;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import dev.ghost.config.GhostConfig;

public final class ClientContext {
    private ClientContext() {}

    public static String collect(Minecraft client, GhostConfig config) {
        if (client.player == null || client.level == null) return "";
        var player = client.player;
        ClientLevel level = client.level;
        StringBuilder out = new StringBuilder();
        if (config.contextVersion) add(out, "Versão", SharedConstants.getCurrentVersion().name());
        if (config.contextDimension) add(out, "Dimensão", level.dimension().identifier().toString());
        if (config.contextY) add(out, "Coordenada Y", Integer.toString(player.getBlockY()));
        if (config.sendExactXZ) add(out, "Coordenadas X/Z (compartilhadas por opção)", player.getBlockX() + ", " + player.getBlockZ());
        if (config.contextBiome) {
            try {
                String biome = level.getBiome(BlockPos.containing(player.position())).unwrapKey().map(key -> key.identifier().toString()).orElse("indisponível");
                add(out, "Bioma", biome);
            } catch (Exception ignored) { add(out, "Bioma", "indisponível"); }
        }
        if (config.contextDifficulty) add(out, "Dificuldade", level.getDifficulty().toString());
        if (config.contextVitals) {
            add(out, "Vida", Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()));
            add(out, "Fome", Integer.toString(player.getFoodData().getFoodLevel()));
        }
        if (config.contextHeldItem) add(out, "Item principal", player.getMainHandItem().isEmpty() ? "mão vazia" : player.getMainHandItem().getHoverName().getString());
        if (config.contextTarget) {
            try {
                if (client.hitResult instanceof BlockHitResult hit && hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    add(out, "Alvo", level.getBlockState(hit.getBlockPos()).getBlock().getName().getString());
                } else if (client.hitResult instanceof EntityHitResult hit) {
                    Entity entity = hit.getEntity();
                    add(out, "Alvo", entity.getType().getDescription().getString());
                }
            } catch (Exception ignored) {}
        }
        if (config.extendedWorldContext) add(out, "Contexto estendido", "não disponível nesta versão; nenhum seed, mapa ou dado de chunks foi coletado");
        return ContextSanitizer.clean(out.toString(), 1400);
    }

    private static void add(StringBuilder out, String label, String value) {
        if (!out.isEmpty()) out.append('\n');
        out.append(label).append(": ").append(ContextSanitizer.clean(value, 180));
    }
}
