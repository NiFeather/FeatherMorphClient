package xyz.nifeather.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class LoadingSpinner extends MDrawable
{
    private static final ResourceLocation LOADING_TEX = ResourceLocation.fromNamespaceAndPath("morphclient", "loading");

    public LoadingSpinner()
    {
        this.setWidth(16);
        this.setHeight(16);
    }

    @Override
    protected void onRender(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        //RenderSystem.enableBlend();

        context.blitSprite(RenderPipelines.GUI_TEXTURED, LOADING_TEX, 0, 0, this.renderWidth, this.renderHeight);
        super.onRender(context, mouseX, mouseY, delta);
    }
}
