package xyz.nifeather.morph.client.graphics;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.color.Colors;

public class DrawableSprite extends MDrawable
{
    private final ResourceLocation textureIdentifier;
    private final boolean isGuiTexture;

    public DrawableSprite(ResourceLocation textureIdentifier, boolean isGuiTexture)
    {
        this.textureIdentifier = textureIdentifier;
        this.isGuiTexture = isGuiTexture;
    }

    public DrawableSprite(ResourceLocation textureIdentifier)
    {
        this(textureIdentifier, true);
    }

    @Override
    protected void onRender(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        int texWidth = Math.round(this.getRenderWidth());
        int texHeight = Math.round(this.getRenderHeight());

        // ARGB
        int color = 0x00FFFFFF;
        int alphaMask = Math.round(alpha.get() * 255) << 24;

        color = color | alphaMask;

        if (isGuiTexture)
        {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, textureIdentifier,
                    0, 0,
                    texWidth, texHeight, color);
        }
        else
        {
            context.blit(RenderPipelines.GUI_TEXTURED, textureIdentifier,
                    0, 0,
                    0, 0,
                    texWidth, texHeight,
                    texWidth, texHeight, color);
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation)
    {
        return null;
    }
}
