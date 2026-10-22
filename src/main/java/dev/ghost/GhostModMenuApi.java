package dev.ghost;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.ghost.ui.GhostConfigScreen;

/** Integração opcional com o Mod Menu: só é carregada se o Mod Menu estiver instalado. */
public final class GhostModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GhostConfigScreen::new;
    }
}
