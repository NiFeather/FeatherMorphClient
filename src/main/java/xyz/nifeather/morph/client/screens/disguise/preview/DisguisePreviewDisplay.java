package xyz.nifeather.morph.client.screens.disguise.preview;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.graphics.EntityDisplay;

public class DisguisePreviewDisplay extends EntityDisplay
{
    public DisguisePreviewDisplay(String rawIdentifier, boolean displayLoadingIfNotValid, InitialSetupMethod initialSetupMethod)
    {
        super(rawIdentifier, displayLoadingIfNotValid, initialSetupMethod);
    }

    public DisguisePreviewDisplay(String id)
    {
        super(id);
    }

    @Override
    protected void onRender(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        var matrices = context.pose();

        matrices.pushMatrix();

        var mX = Math.round(this.getScreenSpaceX() + this.getRenderWidth() / 2f - 30);
        var mY = Math.round(this.getScreenSpaceY() + this.getRenderHeight() / 2f);

        try
        {
            super.onRender(context, mX, mY, delta);
        }
        finally
        {
            matrices.popMatrix();
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation)
    {
        return null;
    }
}
