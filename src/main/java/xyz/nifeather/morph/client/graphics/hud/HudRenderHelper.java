package xyz.nifeather.morph.client.graphics.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.math.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xyz.nifeather.morph.client.ClientMorphManager;
import xyz.nifeather.morph.client.MorphClientObject;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;
import xyz.nifeather.morph.client.graphics.transforms.Recorder;
import xyz.nifeather.morph.client.graphics.transforms.Transformer;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

public class HudRenderHelper extends MorphClientObject
{
    @Resolved
    private ClientMorphManager manager;

    private final Bindable<Float> revValueNext = new Bindable<>(0f);
    private final Recorder<Float> revDisplayRecorder = new Recorder<>(0f);
    private final Recorder<Float> progressHeightRecorder = new Recorder<>(0f);
    private final Recorder<Float> barHeightRecorder = new Recorder<>(0f);

    private final Recorder<Color> colorRecord = new Recorder<>(Color.ofOpaque(0));
    private final Bindable<Color> colorNext = new Bindable<>(Color.ofOpaque(0));

    private final Recorder<Float> drawAlpha = new Recorder<>(0f);

    private final Bindable<Color> preferredBgColor = new Bindable<>(Color.ofOpaque(0));
    private final Recorder<Color> bgColorRecord = new Recorder<>(Color.ofOpaque(0));

    private final Bindable<Boolean> visible = new Bindable<>(false);

    @Initializer
    private void load()
    {
        revValueNext.bindTo(manager.revealingValue);

        colorNext.onValueChanged((o, n) ->
        {
            if (o == null) o = MaterialColors.Indigo500;

            if (!n.equals(o))
                Transformer.transform(colorRecord, n, visible.get() ? 1500 : 0, Easing.OutQuint);
        }, true);

        revValueNext.onValueChanged((o, n) ->
        {
            Transformer.transform(revDisplayRecorder, n, visible.get() ? 2000 : 0, Easing.OutQuint);
        }, true);

        preferredBgColor.onValueChanged((o, n) ->
        {
            if (o == null) o = MaterialColors.Indigo500;

            if (!n.equals(o))
                Transformer.transform(bgColorRecord, n, visible.get() ? 1500 : 0, Easing.OutQuint);
        }, true);

        visible.onValueChanged((o, n) ->
        {
            // Bar进度
            Transformer.transform(progressHeightRecorder, n ? 0f : -2.5f, 650, Easing.InOutQuint);

            // Bar位置
            Transformer.transform(barHeightRecorder, n ? 0f : -6f, 650, Easing.OutBack);

            Transformer.transform(drawAlpha, n ? 1 : 0f, 650, Easing.OutQuint);
        }, true);

        this.addSchedule(this::update);
    }

    private void update()
    {
        this.addSchedule(this::update);

        //if (plugin.getCurrentTick() % 40 == 0)
        //    revValueNext.set(revValueNext.get() > 0f ? 0f : new Random().nextFloat(10, 100));

        var rev = revDisplayRecorder.get();

        var targetClr = (rev >= 20)
                ? (rev >= 80 ? MaterialColors.Red600 : MaterialColors.Orange500)
                : MaterialColors.Green400;

        colorNext.set(targetClr);
        preferredBgColor.set(targetClr.darker(3f));

        visible.set(rev > 0.1f);
    }

    public void onRender(GuiGraphics context, DeltaTracker renderTickCounter)
    {
        if (manager == null || drawAlpha.get() == 0f || Minecraft.getInstance().options.hideGui) return;

        var matrices = context.pose();

        try
        {
            matrices.pushMatrix();

            renderBar(context, renderTickCounter);
        }
        finally
        {
            matrices.popMatrix();
        }
    }

    public void renderBar(GuiGraphics context, DeltaTracker renderTickCounter)
    {
        // 10 * 0.8
        var width = 8;

        // 35 * 0.8
        var height = 28;
        var padding = 1;

        var windowHeight = context.guiHeight();
        var matrices = context.pose();

        var shaderColor = RenderSystem.getShaderColor();
        shaderColor = new float[]
        {
            shaderColor[0],
            shaderColor[1],
            shaderColor[2],
            shaderColor[3]
        };

        RenderSystem.setShaderColor(1, 1, 1, drawAlpha.get());

        // 先位移到屏幕外面
        // 然后再位移到屏幕里面
        matrices.translate(barHeightRecorder.get() + 2, windowHeight - height - 2, matrices);

        context.renderOutline(0, 0, width, height, bgColorRecord.get().darker(1.3).getColor());

        // 填充背景
        context.fill(padding, padding, width - padding, height - padding, bgColorRecord.get().getColor());

        // 填充进度
        var barStart = padding + Math.round((height - padding * 2) * (revDisplayRecorder.get() / 100));
        var barEnd = height - padding;
        context.fill(padding, barStart, width - padding, barEnd, colorRecord.get().getColor());

        RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
    }

    public void renderProgress(GuiGraphics context, DeltaTracker renderTickCounter)
    {
        var width = context.guiWidth();

        var height = 2;
        var matrices = context.pose();

        //logger.info("H " + heightRecorder.get());
        matrices.translate(0, progressHeightRecorder.get(), matrices);

        //var width = context.getScaledWindowWidth();
        var scale = width - Math.round(width * (revDisplayRecorder.get() / 100));
        // var scale30 = Math.round(width * 0.2f);
        // var scale80 = Math.round(width * 0.8f);

        // Base W
        context.fill(0, 0, context.guiWidth(), height, ColorUtils.forOpacity(bgColorRecord.get(), 0.6f).getColor());

        // Progress
        context.fill(0, 0, scale, height, colorRecord.get().getColor());
    }
}
