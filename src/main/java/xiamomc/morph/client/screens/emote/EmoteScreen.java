package xiamomc.morph.client.screens.emote;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.graphics.Anchor;
import xiamomc.morph.client.graphics.MDrawable;
import xiamomc.morph.client.graphics.transforms.Recorder;
import xiamomc.morph.client.graphics.transforms.Transformer;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.morph.client.screens.FeatherScreen;
import xiamomc.morph.client.screens.WaitingForServerScreen;
import xiamomc.morph.client.utilties.MathUtils;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.List;

public class EmoteScreen extends FeatherScreen
{
    private final Bindable<Boolean> serverReady = new Bindable<>();

    public EmoteScreen()
    {
        super(Text.literal("Disguise emote select screen"));

        //addSingleEmoteWidget(0, 0);
        addSingleEmoteWidget(0, -(widgetSize + 5));
        addSingleEmoteWidget(widgetSize + 5, 0);
        addSingleEmoteWidget(0, widgetSize + 5);
        addSingleEmoteWidget(-(widgetSize + 5), 0);

        var morphManager = MorphClient.getInstance().morphManager;
        var emotes = morphManager.getEmotes();
        for (int i = 0; i < emotes.size(); i++)
        {
            if (i >= emoteWidgets.size())
            {
                //logger.warn("We run out of widgets!!!");
                break;
            }

            var widget = emoteWidgets.get(i);
            widget.setEmote(emotes.get(i));
        }

        addSingleEmoteWidget(0, 0).setText(Text.translatable("gui.back"));

        this.alpha.set(0f);
        this.fadeIn(200, Easing.OutQuint);

        var serverHandler = MorphClient.getInstance().serverHandler;
        this.serverReady.bindTo(serverHandler.serverReady);

        this.serverReady.onValueChanged((o, n) ->
        {
            MorphClient.getInstance().schedule(() ->
            {
                if (this.isCurrent() && !n)
                    this.push(new WaitingForServerScreen(new EmoteScreen()));
            });
        }, true);

    }

    @Override
    protected void onScreenExit(@Nullable Screen nextScreen)
    {
        super.onScreenExit(nextScreen);

        if (nextScreen == null)
            this.serverReady.unBindFromTarget();
    }

    @Override
    protected void onScreenResize()
    {
        super.onScreenResize();

        this.mChildren().forEach(e ->
        {
            ((MDrawable)e).invalidatePosition();
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        var shaderColor = RenderSystem.getShaderColor();
        shaderColor = new float[]
                {
                        shaderColor[0],
                        shaderColor[1],
                        shaderColor[2],
                        shaderColor[3]
                };

        context.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3] * this.alpha.get());

        super.render(context, mouseX, mouseY, delta);

        context.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
    }

    //region Alpha

    protected final Recorder<Float> alpha = new Recorder<Float>(1f);

    public void setAlpha(float newVal)
    {
        this.alpha.set(newVal);
    }

    public void fadeTo(float newVal, long duration, Easing easing)
    {
        Transformer.transform(alpha, MathUtils.clamp(0f, 1f, newVal), duration, easing);
    }

    public void fadeIn(long duration, Easing easing)
    {
        this.fadeTo(1, duration, easing);
    }

    public void fadeOut(long duration, Easing easing)
    {
        this.fadeTo(0, duration, easing);
    }

    //endregion Alpha

    private final int widgetSize = 55;

    private final List<SingleEmoteWidget> emoteWidgets = new ObjectArrayList<>();

    private SingleEmoteWidget addSingleEmoteWidget(int xOffset, int yOffset)
    {
        var widget = new SingleEmoteWidget();
        widget.setAnchor(Anchor.Centre);

        widget.setWidth(widgetSize);
        widget.setHeight(widgetSize);

        widget.setX(xOffset);
        widget.setY(yOffset);

        widget.onClick(() -> MorphClient.getInstance().schedule(this::tryClose));

        this.add(widget);
        emoteWidgets.add(widget);

        return widget;
    }

    private void tryClose()
    {
        if (this.isCurrent())
            this.close();
    }
}
