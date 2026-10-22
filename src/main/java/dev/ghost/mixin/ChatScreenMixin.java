package dev.ghost.mixin;

import dev.ghost.GhostClient;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captura apenas a submissão local ao chat, antes de qualquer pacote sair do cliente. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void ghost$interceptPrivatePrompt(String message, boolean addToHistory, CallbackInfo ci) {
        if (GhostClient.handleChatInput(message)) ci.cancel();
    }
}
