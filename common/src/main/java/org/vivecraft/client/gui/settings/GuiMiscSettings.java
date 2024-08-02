package org.vivecraft.client.gui.settings;

import net.minecraft.client.gui.screens.Screen;
import org.vivecraft.client.gui.framework.GuiVROptionsBase;
import org.vivecraft.client.gui.framework.VROptionEntry;
import org.vivecraft.client_vr.settings.VRSettings;

public class GuiMiscSettings extends GuiVROptionsBase {
    private final VROptionEntry[][] miscSettings = {
        new VROptionEntry[]{
            new VROptionEntry(VRSettings.VrOptions.LOW_HEALTH_INDICATOR)
        }
    };

    public GuiMiscSettings(Screen guiScreen) {
        super(guiScreen);
    }

    public void init() {
        this.vrTitle = "vivecraft.options.screen.misc";
        super.init(this.miscSettings[super.currentPage], true);
        super.setTotalPages(miscSettings.length);
        super.addDefaultButtons();
    }
}
