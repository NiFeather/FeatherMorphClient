package xyz.nifeather.morph.client.graphics.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.math.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.MorphClientObject;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.transforms.Recorder;
import xyz.nifeather.morph.client.graphics.transforms.Transformer;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.concurrent.atomic.AtomicBoolean;

public class LinedToast extends MorphClientObject implements Toast
{
    public LinedToast()
    {
    }

    private final Recorder<Integer> outlineWidth = Recorder.of(0);

    protected final Bindable<Visibility> visibility = new Bindable<Toast.Visibility>(Visibility.HIDE);

    protected boolean fadeInOnEnter()
    {
        return false;
    }

    @Initializer
    private void load()
    {
        outlineWidth.set(this.width());

        this.visibility.onValueChanged((o, visible) ->
        {
            if (visible == Visibility.SHOW)
                Transformer.delay(300).then(() ->
                {
                    Transformer.transform(outlineWidth, 2, 600, Easing.OutQuint);
                });
            else
                Transformer.transform(outlineWidth, this.width(), 600, Easing.OutQuad);
        }, true);
    }

    private final AtomicBoolean layoutValid = new AtomicBoolean(false);

    protected void invalidateLayout()
    {
        layoutValid.set(false);
    }

    protected void updateLayout()
    {
        if (title != null)
        {
            Component titleDisplay = Component.literal(textRenderer.substrByWidth(title, this.getTextWidth()).getString());

            if (!titleDisplay.getString().equalsIgnoreCase(title.getString()))
                titleDisplay = Component.nullToEmpty(titleDisplay.getString() + "...");

            this.titleDisplay = titleDisplay;
        }
        else
            this.titleDisplay = Component.literal("Null title");

        if (description != null)
        {
            Component descDisplay = Component.literal(textRenderer.substrByWidth(description, this.getTextWidth()).getString());

            if (!descDisplay.getString().equalsIgnoreCase(description.getString()))
                descDisplay = Component.nullToEmpty(descDisplay.getString() + "...");

            this.descDisplay = descDisplay;
        }
        else
            this.descDisplay = Component.literal("");

        layoutValid.set(true);
    }

    public void setTitle(Component text)
    {
        this.title = text;
        this.invalidateLayout();
    }

    @Nullable
    public Component getTitle()
    {
        return title;
    }

    public void setDescription(Component text)
    {
        this.description = text;
        this.invalidateLayout();
    }

    @Nullable
    public Component getDescription()
    {
        return description;
    }

    private static final Component defaultText = Component.empty();

    private Component title;
    private Component description;
    private Component titleDisplay = defaultText;
    private Component descDisplay = defaultText;
    private Color lineColor = Color.ofRGB(255, 255, 255);

    @NotNull
    public Color getLineColor()
    {
        return lineColor;
    }

    public void setLineColor(@Nullable Color newColor)
    {
        if (newColor == null) newColor = Color.ofRGB(255, 255, 255);
        this.lineColor = newColor;
    }

    private final Font textRenderer = Minecraft.getInstance().font;

    protected void postTextDrawing(GuiGraphics context, long startTime)
    {
    }

    protected void postBackgroundDrawing(GuiGraphics context, long startTime)
    {
    }

    protected void postDraw(GuiGraphics context, long startTime)
    {
    }

    protected boolean drawProgress()
    {
        return FeatherMorphClient.getInstance().getModConfigData().displayToastProgress;
    }

    protected float getTextStartX()
    {
        return this.width() * 0.25F - 4;
    }

    protected int getTextWidth()
    {
        return (int) (this.width() * 0.65F);
    }

    @Override
    public Visibility getWantedVisibility()
    {
        return this.visibility.get();
    }

    /**
     * Range: 0 ~ 1
     */
    private double progress = 0d;

    @Override
    public void update(ToastManager manager, long startTime)
    {
        this.progress = Math.min(1, startTime / (5000.0 * manager.getNotificationDisplayTimeMultiplier()));

        // Update visibility
        var visibility = this.progress >= 1 ? Visibility.HIDE : Visibility.SHOW;
        this.visibility.set(visibility);
    }

    private final Color progressColor = ColorUtils.fromHex("666666");
    private final Color borderColor = ColorUtils.fromHex("#444444");

    @Override
    public void render(GuiGraphics context, Font textRenderer, long startTime)
    {
        if (!layoutValid.get())
            updateLayout();

        var xRightPadding = 1;
        var xLeftPadding = 2;
        var yPadding = 1;

        context.enableScissor(xRightPadding, 0, width(), height());

        // Draw background
        context.fill(xRightPadding, yPadding,
                this.width() - xLeftPadding, this.height() - yPadding, 0xFF333333);

        var matrices = context.pose();

        // Draw progress bar
        if (drawProgress())
        {
            var progressDisplay = Math.max(0, 0.95 - progress);

            matrices.pushMatrix();

            var translateX = this.width() * (1 - progressDisplay);
            matrices.translate((float)-translateX, 0, matrices);

            context.fill(xRightPadding, yPadding,
                    this.width(),
                    this.height() - yPadding,
                    ColorUtils.forOpacity(progressColor, (float)progressDisplay).getColor());

            matrices.popMatrix();
        }

        postBackgroundDrawing(context, startTime);

        // Draw text
        var textStartX = (int)getTextStartX();
        var textStartY = Math.round((this.height()) / 2f) - textRenderer.lineHeight + yPadding;

        context.drawString(textRenderer, titleDisplay, textStartX, textStartY - 1, 0xffffffff);
        context.drawString(textRenderer, descDisplay, textStartX, textStartY + textRenderer.lineHeight + 1, 0xffffffff);

        postTextDrawing(context, startTime);

        // Draw CoverLine
        matrices.pushMatrix();
        matrices.translate(0, 0, matrices);

        var lineWidth = Math.min(outlineWidth.get(), this.width() - xRightPadding);

        context.fill(xRightPadding + 1, yPadding + 1,
                lineWidth, this.height() - yPadding - 1,
                lineColor.getColor());

        context.renderOutline(xRightPadding + 1, yPadding + 1,
                this.width() - xLeftPadding - 2, this.height() - yPadding - 2,
                lineColor.getColor());

        context.renderOutline(xRightPadding, yPadding,
                this.width() - xLeftPadding, this.height() - yPadding,
                borderColor.getColor());

        matrices.popMatrix();

        postDraw(context, startTime);

        context.disableScissor();
    }
}
