package org.vivecraft.client.gui.settings;

import net.minecraft.client.gui.screens.Screen;
import org.vivecraft.client.gui.framework.GuiVROptionsBase;
import org.vivecraft.client.gui.framework.VROptionLayout;

public class GuiLoadDefaultsConfirm extends GuiVROptionsBase {
    public Runnable[] loadDefaultsRunnables;

    private final VROptionLayout[] loadDefaultsConfirm = new VROptionLayout[]{
        new VROptionLayout((button, mousePos) -> {
            this.minecraft.setScreen(this.lastScreen);
            return false;
        }, VROptionLayout.Position.POS_RIGHT, 2.0F, true, "gui.cancel"),
        new VROptionLayout((button, mousePos) -> {
            if (loadDefaultsRunnables != null) {
                for (Runnable callback : loadDefaultsRunnables) {
                    callback.run();
                }
            }
            this.minecraft.setScreen(this.lastScreen);
            return false;
        }, VROptionLayout.Position.POS_LEFT, 2.0F, true, "vivecraft.gui.loaddefaults")
    };

    public GuiLoadDefaultsConfirm(Screen lastScreen) {
        super(lastScreen);
    }

    protected void init() {
        this.vrTitle = "vivecraft.messages.loaddefaultsconfirm";
        super.init(this.loadDefaultsConfirm, true);
    }
}
