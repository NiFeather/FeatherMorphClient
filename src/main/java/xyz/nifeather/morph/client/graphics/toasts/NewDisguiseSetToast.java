package xyz.nifeather.morph.client.graphics.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderPipelines;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xiamomc.pluginbase.Annotations.Initializer;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class NewDisguiseSetToast extends LinedToast
{
    public NewDisguiseSetToast(boolean allGone)
    {
        this.allGone.set(allGone);
    }

    @Override
    protected boolean fadeInOnEnter()
    {
        return true;
    }

    private final AtomicBoolean allGone = new AtomicBoolean(false);

    @Initializer
    private void load()
    {
        var transId = "text.morphclient.toast.new_disguises";
        setTitle(Component.translatable(transId));
        setDescription(Component.translatable(transId + (allGone.get() ? ".all_gone" : ".desc"), Component.keybind("key.morphclient.morph").withStyle(ChatFormatting.ITALIC)));
        setLineColor(ColorUtils.fromHex("#009688"));
    }

    @Override
    public int width()
    {
        var textRenderer = Minecraft.getInstance().font;

        var desc = getDescription();
        var title = getTitle();

        var descLength = textRenderer.width(desc == null ? Component.EMPTY : desc);
        var titleLength = textRenderer.width(title == null ? Component.EMPTY : title);
        var max1 = Math.max(descLength, titleLength) + 20;

        return Math.max(super.width(), max1);
    }

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "textures/gui/sprites/icon/info.png");

    @Override
    protected void postBackgroundDrawing(GuiGraphics context, long startTime)
    {
        super.postBackgroundDrawing(context, startTime);

        //RenderSystem.enableBlend();
        //RenderSystem.setShaderTexture(0, TEX);

        context.blitSprite(RenderPipelines.GUI_TEXTURED, TEX, this.width() / 16 - 2, 6, 20, 20);
    }
}
