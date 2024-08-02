package org.vivecraft.client.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.vivecraft.client.gui.settings.GuiMainVRSettings;

public class VivecraftModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GuiMainVRSettings::new;
    }
}
