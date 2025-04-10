package xyz.nifeather.morph.client.screens.disguise;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.render.GuiLayer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nifeather.morph.client.ClientMorphManager;
import xyz.nifeather.morph.client.EntityCache;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.client.graphics.*;
import xyz.nifeather.morph.client.graphics.color.Colors;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;
import xyz.nifeather.morph.client.graphics.container.*;
import xyz.nifeather.morph.client.graphics.transforms.Recorder;
import xyz.nifeather.morph.client.mixin.InventoryScreenMixin;
import xyz.nifeather.morph.client.screens.FeatherScreen;
import xyz.nifeather.morph.client.screens.WaitingForServerScreen;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.client.screens.disguise.preview.DisguisePreviewDisplay;

import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class DisguiseScreen extends FeatherScreen
{
    public DisguiseScreen()
    {
        super(Component.literal("选择界面"));

        var morphClient = FeatherMorphClient.getInstance();

        this.serverHandler = morphClient.serverHandler;
        this.manager = morphClient.morphManager;

        manager.onMorphGrant(c ->
        {
            if (!this.isCurrent()) return false;

            morphClient.schedule(() ->
            {
                var availableMorphs = manager.getAvailableMorphs();
                c.forEach(s -> disguiseList.children().add(availableMorphs.indexOf(s) + 1, new EntityDisplayEntry(s)));
            });

            return true;
        });

        manager.onMorphRevoke(c ->
        {
            if (!this.isCurrent()) return false;

            morphClient.schedule(() ->
                    c.forEach(s -> disguiseList.children().removeIf(w -> w.getIdentifierString().equals(s))));

            return true;
        });

        this.selectedIdentifier.bindTo(manager.selectedIdentifier);
        this.selectedIdentifier.onValueChanged((o, n) ->
        {
            this.refreshEntityPreview(n);
        }, true);

        this.serverReady.bindTo(serverHandler.serverReady);

        this.serverReady.onValueChanged((o, n) ->
        {
            FeatherMorphClient.getInstance().schedule(() ->
            {
                if (this.isCurrent() && !n)
                    this.push(new WaitingForServerScreen(new DisguiseScreen()));
            });
        }, true);

        this.currentIdentifier.bindTo(manager.currentIdentifier);

        //初始化文本
        this.currentIdentifier.onValueChanged((o, n) ->
        {
            Component display = null;

            if (n != null)
            {
                var cachedEntity = EntityCache.getGlobalCache().getEntity(n, null);

                if (cachedEntity != null)
                    display = cachedEntity.getName();
                else
                    display = Component.literal(n);
            }

            var text = Component.translatableEscape("gui.morphclient.current_disguise",
                    display == null ? Component.translatable("gui.none") : display);

            selectedIdentifierText.setText(text);
            refreshEntityPreview(n);
        }, true);

        serverAPIText.setColor(0x99ffffff);

        titleText.setWidth(200);
        titleText.setHeight(20);

        textBox = new TextFieldWidgetWrapper(new EditBox(Minecraft.getInstance().font, 120, 17, Component.literal("Search disguise...")));
        textBox.setChangedListener(this::onTextBoxText);

        textBox.setAnchor(Anchor.TopRight);
        textBox.setMargin(new MarginPadding(5));
        //textBox.setX(Math.round(-textBox.getWidth()));
    }

    @Nullable
    private EntityDisplay playerDisplay;

    private void refreshEntityPreview(String newId)
    {
        String identifier = newId == null
                            ? this.currentIdentifier.get() == null
                                ? FeatherMorphClient.UNMORPH_STIRNG
                                : this.currentIdentifier.get()
                            : newId;

        if (playerDisplay != null)
        {
            this.remove(playerDisplay);
            playerDisplay.dispose();
            playerDisplay = null;
        }

        var newDisplay = new DisguisePreviewDisplay(identifier, true, EntityDisplay.InitialSetupMethod.SYNC);
        newDisplay.setMasking(true);

        newDisplay.setParentScreenSpace(new ScreenRectangle(ScreenPosition.of(ScreenAxis.HORIZONTAL, 0, 0), this.width, this.height));
        newDisplay.setRelativeSizeAxes(Axes.Both);
        newDisplay.setAnchor(Anchor.CentreRight);
        newDisplay.setSize(new UVPair(0.4f, 0.7f));

        playerDisplay = newDisplay;

        this.add(newDisplay);
    }

    private final Bindable<String> selectedIdentifier = new Bindable<>(FeatherMorphClient.UNMORPH_STIRNG);
    private final Bindable<Boolean> serverReady = new Bindable<>(false);
    private final Bindable<String> currentIdentifier = new Bindable<>(FeatherMorphClient.UNMORPH_STIRNG);

    private final TextFieldWidgetWrapper textBox;

    private final ClientMorphManager manager;
    private final ServerHandler serverHandler;

    private final Container topTextContainer = new Container();
    private final Container bottomTextContainer = new Container();

    private final DisguiseList disguiseList = new DisguiseList(Minecraft.getInstance(), 200, 0, 20, 0, 22);
    private final DrawableText titleText = new DrawableText(Component.translatable("gui.morphclient.select_disguise"));
    private final DrawableText selectedIdentifierText = new DrawableText();
    private final DrawableText serverAPIText = new DrawableText();
    private final DrawableText outdatedText = new DrawableText(Component.translatable("gui.morphclient.version_mismatch").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD));

    private final int fontMargin = 4;

    @Override
    protected void onScreenExit(Screen next)
    {
        super.onScreenExit(next);

        if (next == null)
        {
            if (fullList != null)
            {
                disguiseList.clearChildren(false);
                disguiseList.children().addAll(fullList);
                fullList = null;
            }

            //workaround: Bindable在界面关闭后还是会保持引用，得手动把字段设置为null
            disguiseList.clearChildren();

            this.serverReady.unBindFromTarget();
            this.selectedIdentifier.unBindFromTarget();
            this.currentIdentifier.unBindFromTarget();
        }
    }

    private final Bindable<Integer> topHeight = new Bindable<>(0);
    private final Bindable<Integer> bottomHeight = new Bindable<>(0);
    private final Recorder<Float> backgroundDim = new Recorder<>(0f);

    public float getBackgroundDim()
    {
        return backgroundDim.get();
    }

    @Override
    protected void onScreenEnter(Screen last)
    {
        super.onScreenEnter(last);

        resizeDisguiseList();

        if (last == null || last instanceof WaitingForServerScreen)
        {
            disguiseList.addChild(new EntityDisplayEntry(FeatherMorphClient.UNMORPH_STIRNG));

            manager.getAvailableMorphs().forEach(s -> disguiseList.addChild(new EntityDisplayEntry(s)));

            //第一次打开时滚动到当前伪装
            scrollToCurrentOrLast(false);
        }

        if (last instanceof WaitingForServerScreen waitingForServerScreen)
            backgroundDim.set(waitingForServerScreen.getCurrentDim());

        int headerTargetHeight = font.lineHeight * 2 + fontMargin * 2;

        topHeight.set(headerTargetHeight);
        bottomHeight.set(30);
        backgroundDim.set(0.3f);

        topTextContainer.addRange(titleText, selectedIdentifierText);
        topTextContainer.setPadding(new MarginPadding(0, 0, fontMargin - 1, 0));

        if (!FeatherMorphClient.getInstance().serverHandler.serverApiMatch())
            bottomTextContainer.add(outdatedText);

        bottomTextContainer.add(serverAPIText);
        bottomTextContainer.setAnchor(Anchor.BottomLeft);
        bottomTextContainer.setPadding(new MarginPadding(0, 0, fontMargin + 1, 0));

        var fontHeight = font.lineHeight;
        serverAPIText.setMargin(new MarginPadding(0, 0, fontHeight + 2, 0));
        selectedIdentifierText.setMargin(new MarginPadding(0, 0, fontHeight + 2, 0));

        var containerHeight = 30;
        bottomTextContainer.setHeight(containerHeight);
        topTextContainer.setHeight(containerHeight);

        var buttonContainer = new BasicContainer<MDrawable>();

        //初始化按钮
        var closeButton = this.createDrawableWrapper(0, 0, 112, 20, Component.translatable("gui.back"), (button) ->
        {
            this.onClose();
        });

        var configMenuButton = this.createDrawableWrapper(0, 0, 20, 20, Component.literal("C"), (button ->
        {
            var screen = FeatherMorphClient.getInstance().getFactory(this).build();

            Minecraft.getInstance().setScreen(screen);
        }));

        var selfVisibleToggle = this.createToggleSelfButton();

        selfVisibleToggle.setX(Math.round(configMenuButton.getWidth() + 5));
        closeButton.setX(selfVisibleToggle.getX() + (int)selfVisibleToggle.getWidth() + 5);

        buttonContainer.addRange(closeButton, selfVisibleToggle, configMenuButton);
        buttonContainer.setAnchor(Anchor.BottomRight);
        buttonContainer.setSize(new UVPair(closeButton.getX() + closeButton.getWidth(), 20));
        buttonContainer.setMargin(new MarginPadding(5));

        var topHeader = new DrawableSprite(Screen.INWORLD_HEADER_SEPARATOR, false);
        topHeader.setY(this.topHeight.get() - 2);
        topHeader.setSize(new UVPair(1, 2));
        topHeader.setRelativeSizeAxes(Axes.X);

        var bottomFooter = new DrawableSprite(Screen.INWORLD_FOOTER_SEPARATOR, false);
        bottomFooter.setAnchor(Anchor.BottomLeft);
        bottomFooter.setY(-bottomHeight.get() + 1);
        bottomFooter.setSize(new UVPair(1, 2));
        bottomFooter.setRelativeSizeAxes(Axes.X);

        //顶端文本
        var screenX = 30;

        topTextContainer.setX(screenX);
        bottomTextContainer.setX(screenX);
        textBox.setX(-25);
        buttonContainer.setX(-25);

        serverAPIText.setText("C %s :: S %s".formatted(serverHandler.getImplmentingApiVersion(), serverHandler.getServerApiVersion()));

        this.addRange(new IMDrawable[]
        {
                disguiseList,
                topTextContainer,
                bottomTextContainer,
                buttonContainer,
                textBox,
                topHeader,
                bottomFooter
        });
    }

    private DrawableButtonWrapper createToggleSelfButton()
    {
        var bindable = manager.selfVisibleEnabled;

        Function<Boolean, Component> textFunction = val ->
        {
            var color = val
                        ? TextColor.fromLegacyFormat(ChatFormatting.GREEN)
                        : TextColor.fromLegacyFormat(ChatFormatting.RED);

            return Component.literal(val ? "I" : "O")
                    .setStyle(Style.EMPTY.withColor(color));
        };

        var button = this.buildButtonWidget(0, 0, 20, 20, textFunction.apply(bindable.get()), btn ->
        {
            var val = !bindable.get();
            bindable.set(val);

            var modInstance = FeatherMorphClient.getInstance();
            var config = modInstance.getModConfigData();

            modInstance.updateClientView(config.allowClientView, val);
            btn.setMessage(textFunction.apply(val));
        });

        return new DrawableButtonWrapper(button);
    }

    private void resizeDisguiseList()
    {
        int headerTargetHeight = font.lineHeight * 2 + fontMargin * 2;
        int footerTargetHeight = 30;

        int disguiseListWidth = Math.round(this.width * 0.6f);
        disguiseList.setWidth(disguiseListWidth);
        disguiseList.setHeight(this.height - headerTargetHeight - footerTargetHeight);
        disguiseList.setY(headerTargetHeight);
    }

    @Override
    protected void onScreenResize()
    {
        assert this.minecraft != null;

        //列表
        resizeDisguiseList();

        bottomTextContainer.invalidatePosition();
        topTextContainer.invalidatePosition();
    }

    private void scrollToCurrentOrLast(boolean scrollToLastIfNoCurrent)
    {
        var filter = disguiseList.children();

        var current = manager.currentIdentifier.get();

        if (current != null)
        {
            var widget = disguiseList.children().stream()
                    .filter(w -> current.equals(w.getIdentifierString())).findFirst().orElse(null);

            if (widget != null)
            {
                disguiseList.scrollTo(widget);

                return;
            }
        }

        if (!scrollToLastIfNoCurrent) return;

        EntityDisplayEntry last = null;
        if (!filter.isEmpty())
            last = filter.getLast();

        if (last != null)
            disguiseList.scrollTo(last);
    }

    private final class_114514 field_1919810 = new class_114514(this);

    private void onTextBoxText(String str)
    {
        if (!str.startsWith("!"))
        {
            textBox.widget.setTextColor(Colors.WHITE.getColor());
            applySearch(str);
        }
        else
        {
            textBox.widget.setTextColor(MaterialColors.Amber500.getColor());
            field_1919810.apply(str);
        }
    }

    //todo: Refactor this to use another list instance.
    private void applySearch(String str)
    {
        if (str.isEmpty())
        {
            if (fullList != null)
            {
                disguiseList.clearChildren(false);
                disguiseList.addChildrenRange(fullList);

                fullList = null;
            }

            return;
        }

        if (fullList == null)
            this.fullList = new ObjectArrayList<>(disguiseList.children());

        //搜索id和已加载伪装的实体名称
        var finalStr = str.toLowerCase().trim();
        var filter = fullList.stream().filter(w ->
                        w.getIdentifier().getPath().contains(finalStr)
                                || w.getEntityName().contains(finalStr))
                .toList();

        disguiseList.clearChildren(false);
        disguiseList.addChildrenRange(filter);

        if (disguiseList.scrollAmount() > disguiseList.maxScrollAmount())
            scrollToCurrentOrLast(true);
    }

    /**
     * 搜索前伪装列表中的所有元素
     */
    private List<EntityDisplayEntry> fullList;

    @Override
    protected void init()
    {
        super.init();
        this.setFocused(textBox);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        super.renderBackground(context, mouseX, mouseY, delta);

        context.blit(RenderPipelines.GUI_TEXTURED, Screen.MENU_BACKGROUND,
                0, 0,
                0, -topHeight.get(),
                this.width, this.topHeight.get(), 32, 32);

        context.blit(RenderPipelines.GUI_TEXTURED, Screen.MENU_BACKGROUND,
                0, this.height - bottomHeight.get(),
                0, 0,
                this.width, this.height, 32, 32);
    }
}
