package xyz.nifeather.morph.client.screens.emote;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import xyz.nifeather.morph.client.screens.TestScreen;
import xyz.nifeather.morph.shared.AnimationNames;
import xyz.nifeather.morph.client.ClientMorphManager;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.client.graphics.Anchor;
import xyz.nifeather.morph.client.graphics.DrawableText;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;
import xyz.nifeather.morph.client.screens.spinner.SpinnerScreen;
import xyz.nifeather.morph.client.screens.WaitingForServerScreen;
import xiamomc.morph.network.commands.C2S.C2SAnimationCommand;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EmoteScreen extends SpinnerScreen<SingleEmoteWidget>
{
    private final Bindable<Boolean> serverReady = new Bindable<>();

    private final DrawableText titleText = new DrawableText();

    private final DrawableText currentAnimText = new DrawableText();

    @Override
    protected SingleEmoteWidget createWidget()
    {
        return new SingleEmoteWidget();
    }

    private int getWidgetSize()
    {
        return 55;
    }

    @Override
    protected int getWidgetWidth()
    {
        return getWidgetSize();
    }

    @Override
    protected int getWidgetHeight()
    {
        return getWidgetSize();
    }

    public EmoteScreen()
    {
        super(Component.literal("Disguise emote select screen"));

        serverHandler = FeatherMorphClient.getInstance().serverHandler;
        morphManager = FeatherMorphClient.getInstance().morphManager;

        titleText.setText(Component.translatable("gui.morphclient.emote_select"));
        titleText.setAnchor(Anchor.TopCentre);
        titleText.setDrawShadow(true);

        currentAnimText.setAnchor(Anchor.BottomCentre);
        currentAnimText.setDrawShadow(true);

        var size = this.getWidgetSize();

        var widgetOffset = size + 5;

        addEmoteWidget(0, -widgetOffset * 2).moveTo(new Vector2i(0, -widgetOffset), 300, Easing.OutExpo);
        addEmoteWidget(widgetOffset * 2, 0).moveTo(new Vector2i(widgetOffset, 0), 300, Easing.OutExpo);
        addEmoteWidget(0, widgetOffset * 2).moveTo(new Vector2i(0, widgetOffset), 300, Easing.OutExpo);
        addEmoteWidget(-widgetOffset * 2, 0).moveTo(new Vector2i(-widgetOffset, 0), 300, Easing.OutExpo);

        var morphManager = FeatherMorphClient.getInstance().morphManager;
        var emotes = morphManager.getEmotes();
        for (int i = 0; i < emotes.size(); i++)
        {
            if (i >= spinnerWidgets.size())
            {
                //logger.warn("We run out of widgets!!!");
                break;
            }

            var widget = spinnerWidgets.get(i);
            widget.setEmote(emotes.get(i));
        }

        addEmoteWidget(0, 0).setText(Component.translatable("gui.back"));
        this.add(titleText);
        this.add(currentAnimText);

        var serverHandler = FeatherMorphClient.getInstance().serverHandler;
        this.serverReady.bindTo(serverHandler.serverReady);

        this.serverReady.onValueChanged((o, n) ->
        {
            FeatherMorphClient.getInstance().schedule(() ->
            {
                if (this.isCurrent() && !n)
                    this.push(new WaitingForServerScreen(new EmoteScreen()));
            });
        }, true);

        updateEmoteText(morphManager.emoteDisplayName);
    }

    @Override
    protected void onScreenEnter(@Nullable Screen lastScreen)
    {
        this.push(new TestScreen());
        super.onScreenEnter(lastScreen);
    }

    private final ServerHandler serverHandler;

    private SingleEmoteWidget addEmoteWidget(int x, int y)
    {
        var widget = this.addSingleWidget(x, y);

        widget.setX(x);
        widget.setY(y);

        widget.onClick(() ->
        {
            var emote = widget.getEmote();

            if (emote != null)
                serverHandler.sendCommand(new C2SAnimationCommand(emote));

            FeatherMorphClient.getInstance().schedule(this::tryClose);
        });

        return widget;
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

        titleText.setY((int)Math.round(this.height * 0.07));
        currentAnimText.setY(-(int)Math.round(this.height * 0.07));
    }

    private Component getEmoteText(@Nullable String identifier)
    {
        if (identifier == null || identifier.equals(AnimationNames.VANISH) || identifier.equals(AnimationNames.NONE))
             return Component.translatable("gui.none");

        return Component.translatable("emote.morphclient." + identifier);
    }

    private void updateEmoteText(@Nullable String identifier)
    {
        var text = this.getEmoteText(identifier);
        this.currentAnimText.setText(Component.translatable("gui.morphclient.current_emote", text));
    }

    @Nullable
    private String emoteName;

    private final ClientMorphManager morphManager;

    @Override
    public void tick()
    {
        var managerLast = morphManager.emoteDisplayName;

        if (!Objects.equals(this.emoteName, managerLast))
        {
            this.emoteName = managerLast;
            updateEmoteText(managerLast);
        }

        super.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (FeatherMorphClient.getInstance().getEmoteKeyBind().matches(keyCode, scanCode))
            FeatherMorphClient.getInstance().schedule(this::tryClose);

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void tryClose()
    {
        if (this.isCurrent())
            this.onClose();
    }
}
