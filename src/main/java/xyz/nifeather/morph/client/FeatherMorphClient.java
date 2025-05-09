package xyz.nifeather.morph.client;

import com.llamalad7.mixinextras.sugar.Share;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.XiaMoJavaPlugin;
import xyz.nifeather.morph.FeatherMorphFabricInitializer;
import xyz.nifeather.morph.client.config.ModConfigData;
import xyz.nifeather.morph.client.graphics.EntityRendererHelper;
import xyz.nifeather.morph.client.graphics.ModelWorkarounds;
import xyz.nifeather.morph.client.graphics.hud.HudRenderHelper;
import xyz.nifeather.morph.client.graphics.toasts.DisguiseEntryToast;
import xyz.nifeather.morph.client.graphics.toasts.RequestToast;
import xyz.nifeather.morph.client.screens.WaitingForServerScreen;
import xyz.nifeather.morph.client.screens.disguise.DisguiseScreen;
import xyz.nifeather.morph.client.screens.emote.EmoteScreen;
import xyz.nifeather.morph.client.syncers.DisguiseSyncer;
import xyz.nifeather.morph.client.syncers.animations.AnimHandlerIndex;
import xiamomc.morph.network.Constants;
import xiamomc.morph.network.commands.C2S.*;
import xiamomc.morph.network.commands.S2C.S2CRequestCommand;
import xyz.nifeather.morph.shared.SharedValues;

import java.io.File;
import java.util.List;

@Environment(EnvType.CLIENT)
public class FeatherMorphClient extends XiaMoJavaPlugin implements ClientModInitializer
{
    private static FeatherMorphClient instance;

    public static FeatherMorphClient getInstance()
    {
        return instance;
    }

    public static final String UNMORPH_STIRNG = "morph:unmorph";

    public static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph$Client");

    @Override
    public String namespace()
    {
        return getClientNameSpace();
    }

    public static String getClientNameSpace()
    {
        return "morphclient";
    }

    @Override
    public Logger getSLF4JLogger()
    {
        return LOGGER;
    }

    public FeatherMorphClient()
    {
        instance = this;
    }

    @Nullable
    private Runnable mainLoopRunnable;

    @Override
    public void startMainLoop(Runnable r)
    {
        mainLoopRunnable = r;
    }

    @Override
    public void runAsync(Runnable r)
    {
        Util.backgroundExecutor().execute(r);
    }

    @Override
    public @NotNull File getDataFolder()
    {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), "feathermorphclient-fabric");
    }

    public ClientMorphManager morphManager;
    public ServerHandler serverHandler;
    private ClientSkillHandler skillHandler;
    private DisguiseInstanceTracker disguiseTracker;

    private final AnimHandlerIndex animHandlerIndex = new AnimHandlerIndex();

    private final boolean debugToasts = false;

    @Override
    public void onInitializeClient()
    {
        enablePlugin();

        Constants.initialize(false);

        this.registerKeys();

        dependencyManager.cache(this);
        dependencyManager.cache(disguiseTracker = new DisguiseInstanceTracker());
        dependencyManager.cache(morphManager = new ClientMorphManager());
        dependencyManager.cache(serverHandler = new ServerHandler(this));
        dependencyManager.cache(skillHandler = new ClientSkillHandler());
        dependencyManager.cache(getModConfigData());
        dependencyManager.cache(new ClientRequestManager());
        dependencyManager.cache(new EntityRendererHelper());
        dependencyManager.cache(animHandlerIndex);

        serverHandler.initializeNetwork();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientTickEvents.END_WORLD_TICK.register(this::postWorldTick);
        HudRenderCallback.EVENT.register(hudRenderHelper::onRender);

        modelWorkarounds = ModelWorkarounds.getInstance();
    }

    private final HudRenderHelper hudRenderHelper = new HudRenderHelper();

    private ModelWorkarounds modelWorkarounds;

    private void postWorldTick(ClientLevel clientWorld)
    {
        var syncersToRemove = new ObjectArrayList<DisguiseSyncer>();

        disguiseTracker.getAllSyncer().forEach(syncer ->
        {
            if (syncer.disposed()) syncersToRemove.add(syncer);
            else syncer.onGameTick();
        });

        syncersToRemove.forEach(syncer -> disguiseTracker.removeSyncer(syncer));
    }

    @Nullable
    private Boolean attackPressedDown = null;

    private KeyMapping toggleselfKeyBind;
    private KeyMapping executeSkillKeyBind;
    private KeyMapping unMorphKeyBind;
    private KeyMapping morphKeyBind;
    private KeyMapping resetCacheKeybind;
    private KeyMapping displayOwnerBind;
    private KeyMapping emoteKeyBind;

    public KeyMapping getEmoteKeyBind()
    {
        return emoteKeyBind;
    }

    private KeyMapping testKeyBindGrant;
    private KeyMapping testKeyBindLost;

    private final int keybindCount = 4;
    private final List<KeyMapping> quickDisguiseKeys = new ObjectArrayList<>(keybindCount);

    private void registerKeys()
    {
        //初始化按键
        executeSkillKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphclient.skill", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V, "category.morphclient.keybind"
        ));

        unMorphKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphclient.unmorph", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN, "category.morphclient.keybind"
        ));

        if (debugToasts)
        {
            testKeyBindGrant = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.morphclient.testToastGrant", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_Z, "category.morphclient.keybind"
            ));

            testKeyBindLost = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.morphclient.testToastLost", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_X, "category.morphclient.keybind"
            ));

            for (int i = 1; i <= keybindCount; i++)
            {
                var key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                        "key.morphclient.quick_disguise.%s".formatted(i),
                        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.morphclient.keybind"
                ));
                quickDisguiseKeys.add(key);
            }
        }

        morphKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphclient.morph", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N, "category.morphclient.keybind"
        ));

        toggleselfKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphclient.toggle", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT, "category.morphclient.keybind"
        ));

        emoteKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphclient.emote", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G, "category.morphclient.keybind"
        ));

        resetCacheKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphclient.reset_cache", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, "category.morphclient.keybind"
        ));

        displayOwnerBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphclient.display_name", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8, "category.morphclient.keybind"
        ));
    }

    private void updateKeys(Minecraft client)
    {
        while (executeSkillKeyBind.consumeClick() && skillHandler.getCurrentCooldown() <= 0)
        {
            skillHandler.setSkillCooldown(skillHandler.getSkillCooldown());
            serverHandler.sendCommand(new C2SSkillCommand());
        }

        while (unMorphKeyBind.consumeClick())
            serverHandler.sendCommand(new C2SUnmorphCommand());

        var attackPressed = Minecraft.getInstance().options.keyAttack.isDown();

        if (attackPressed != Boolean.TRUE.equals(this.attackPressedDown))
        {
            var clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer != null && attackPressed)
            {
                var syncer = DisguiseInstanceTracker.getInstance().getSyncerFor(clientPlayer);

                if (syncer != null)
                    syncer.playAttackAnimation();
            }

            this.attackPressedDown = attackPressed;
        }

        if (displayOwnerBind.consumeClick())
        {
            var doRender = !EntityRendererHelper.doRenderRealName;
            EntityRendererHelper.doRenderRealName = doRender;

            var clientPlayer = client.player;
            if (clientPlayer != null)
                clientPlayer.displayClientMessage(Component.translatable("text.morphclient." + (doRender ? "display" : "hide") + "_real_names"), false);
        }

        if (debugToasts)
        {
            if (testKeyBindGrant.consumeClick())
            {
                var toasts = client.getToastManager();
                var morphs = morphManager.getAvailableMorphs();
                var random = RandomSource.create();
                var id = morphs.get(random.nextIntBetweenInclusive(0, morphs.size() - 1));

                toasts.addToast(new DisguiseEntryToast(id, true));
            }

            if (testKeyBindLost.consumeClick())
            {
                var toasts = client.getToastManager();
                toasts.addToast(new RequestToast(S2CRequestCommand.Type.RequestSend, "Very_Loooong_Nammmmmme"));
            }

        }

        while (toggleselfKeyBind.consumeClick())
        {
            var config = getModConfigData();

            boolean val = !morphManager.selfVisibleEnabled.get();

            updateClientView(config.allowClientView, val);
        }

        while (morphKeyBind.consumeClick())
        {
            var player = client.player;

            if (player != null && player.input != null && player.input.keyPresses.shift())
            {
                serverHandler.sendCommand(new C2SMorphCommand(null));
            }
            else if (client.screen == null)
            {
                client.setScreen(new WaitingForServerScreen(new DisguiseScreen()));
            }
        }

        while (resetCacheKeybind.consumeClick())
        {
            EntityCache.getGlobalCache().clearCache();
            modelWorkarounds.initWorkarounds();
        }

        while (emoteKeyBind.consumeClick())
            Minecraft.getInstance().setScreen(new WaitingForServerScreen(new EmoteScreen()));

        for (int i = 0; i < this.quickDisguiseKeys.size(); i++)
        {
            var key = quickDisguiseKeys.get(i);

            while (key.consumeClick())
                morphManager.onQuickDisguise(i);
        }
    }

    @Nullable
    private Boolean lastClientView = null;

    public void updateClientView(boolean clientViewEnabled, boolean selfViewVisible)
    {
        if (lastClientView == null || clientViewEnabled != lastClientView)
        {
            serverHandler.sendCommand(new C2SToggleSelfCommand(clientViewEnabled ? C2SToggleSelfCommand.SelfViewMode.CLIENT_ON : C2SToggleSelfCommand.SelfViewMode.CLIENT_OFF));
            lastClientView = clientViewEnabled;
        }

        serverHandler.sendCommand(new C2SToggleSelfCommand(C2SToggleSelfCommand.SelfViewMode.fromBoolean(selfViewVisible)));

        getModConfigData().allowClientView = clientViewEnabled;
    }

    public void sendMorphCommand(String id)
    {
        if (id == null) id = UNMORPH_STIRNG;

        if (UNMORPH_STIRNG.equals(id))
            serverHandler.sendCommand(new C2SUnmorphCommand());
        else
            serverHandler.sendCommand(new C2SMorphCommand(id));
    }

    //region Config

    private void onConfigSave()
    {
        FeatherMorphFabricInitializer.instance().configHolder.save();
    }

    public ModConfigData getModConfigData()
    {
        return FeatherMorphFabricInitializer.instance().modConfigData;
    }

    public ConfigBuilder getFactory(Screen parent)
    {
        ConfigBuilder builder = ConfigBuilder.create();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory categoryGeneral = builder.getOrCreateCategory(Component.translatable("stat.generalButton"));

        var modConfigData = getModConfigData();

        categoryGeneral.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.previewInInventory.name"), modConfigData.alwaysShowPreviewInInventory)
                        .setTooltip(Component.translatable("option.morphclient.previewInInventory.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.alwaysShowPreviewInInventory = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.changeCameraHeight.name"), modConfigData.changeCameraHeight)
                        .setTooltip(Component.translatable("option.morphclient.changeCameraHeight.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.changeCameraHeight = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.allowClientView.name"), modConfigData.allowClientView)
                        .setTooltip(Component.translatable("option.morphclient.allowClientView.description"))
                        .setDefaultValue(true)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.allowClientView = v;

                            if (serverHandler.serverReady())
                                updateClientView(v, morphManager.selfVisibleEnabled.get());
                        })
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.displayDisguiseOnHud.name"), modConfigData.displayDisguiseOnHud)
                        .setTooltip(Component.translatable("option.morphclient.displayDisguiseOnHud.description"))
                        .setDefaultValue(true)
                        .setSaveConsumer(v ->
                        {
                            modConfigData.displayDisguiseOnHud = v;

                            if (serverHandler.serverReady())
                                serverHandler.sendCommand(new C2SOptionCommand(C2SOptionCommand.ClientOptions.HUD).setValue(v));
                        })
                        .build()
        ).addEntry(
                entryBuilder.startFloatField(Component.translatable("option.morphclient.scrollSpeed.name"), modConfigData.scrollSpeed)
                        .setTooltip(Component.translatable("option.morphclient.scrollSpeed.description"))
                        .setMax(4F)
                        .setMin(0.5F)
                        .setDefaultValue(2.5f)
                        .setSaveConsumer(v -> modConfigData.scrollSpeed = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.verbosePackets.name"), modConfigData.verbosePackets)
                        .setTooltip(Component.translatable("option.morphclient.verbosePackets.description"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.verbosePackets = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.nametag_scaling"), modConfigData.scaleNameTag)
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.scaleNameTag = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.smoothscroll.name"), modConfigData.disguiseListSmoothScroll)
                        .setTooltip(Component.translatable("option.morphclient.smoothscroll.description"))
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> modConfigData.disguiseListSmoothScroll = v)
                        .build()
        );

        ConfigCategory categoryToast = builder.getOrCreateCategory(Component.translatable("category.morphclient.config_toasts"));

        categoryToast.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.grant_revoke_toasts"), modConfigData.displayGrantRevokeToast)
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> modConfigData.displayGrantRevokeToast = v)
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.query_set_toasts"), modConfigData.displayQuerySetToast)
                        .setTooltip(Component.translatable("option.morphclient.query_set_toasts.desc"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> modConfigData.displayQuerySetToast = v)
                        .build())
                .addEntry(
                        entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.toast_progress"), modConfigData.displayToastProgress)
                                .setDefaultValue(false)
                                .setSaveConsumer(v -> modConfigData.displayToastProgress = v)
                                .build());

        ConfigCategory debugCategory = builder.getOrCreateCategory(Component.translatable("category.morphclient.debug"));
        debugCategory.setDescription(new FormattedText[] {Component.translatable("category.morphclient.debug.description")});

        debugCategory.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.singleplayer_debug"), SharedValues.allowSinglePlayerDebugging)
                        .setTooltip(Component.translatable("option.morphclient.singleplayer_debug.tooltip"))
                        .setDefaultValue(false)
                        .setSaveConsumer(v ->
                                {
                                    SharedValues.allowSinglePlayerDebugging = v;
                                    modConfigData.singlePlayerDebugging = v;
                                })
                        .build()
        ).addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.morphclient.pip_workaround"), SharedValues.applyPictureInPictureWorkaround)
                        .setDefaultValue(true)
                        .setSaveConsumer(v ->
                        {
                            SharedValues.applyPictureInPictureWorkaround = v;
                            modConfigData.enablePictureInPictureWorkaround = v;
                        })
                        .build()
        );

        builder.setParentScreen(parent)
                .setTitle(Component.translatable("title.morphclient.config"))
                .transparentBackground();

        builder.setSavingRunnable(this::onConfigSave);

        return builder;
    }

    //endregion Config

    //region tick相关

    private void tick(Minecraft client)
    {
        if (mainLoopRunnable != null)
            mainLoopRunnable.run();

        this.updateKeys(client);
    }

    //endregion tick相关
}
