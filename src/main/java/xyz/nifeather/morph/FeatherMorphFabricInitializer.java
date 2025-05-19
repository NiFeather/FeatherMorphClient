package xyz.nifeather.morph;

import com.llamalad7.mixinextras.sugar.Share;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;
import xyz.nifeather.morph.client.config.ModConfigData;
import xyz.nifeather.morph.server.MorphServerLoader;
import xyz.nifeather.morph.shared.SharedValues;
import xyz.nifeather.morph.shared.commands.arguments.RelaxedStringArgumentType;
import xyz.nifeather.morph.shared.payload.*;
import xyz.nifeather.pipfix.Values;

public class FeatherMorphFabricInitializer implements ModInitializer
{
    private final MorphServerLoader morphServerLoader = new MorphServerLoader();

    public ModConfigData modConfigData;
    public ConfigHolder<ModConfigData> configHolder;

    public static FeatherMorphFabricInitializer instance()
    {
        return INSTANCE;
    }

    private static FeatherMorphFabricInitializer INSTANCE;

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize()
    {
        INSTANCE = this;

        // 初始化配置
        AutoConfig.register(ModConfigData.class, GsonConfigSerializer::new);

        configHolder = AutoConfig.getConfigHolder(ModConfigData.class);
        configHolder.load();

        modConfigData = configHolder.getConfig();

        SharedValues.allowSinglePlayerDebugging = modConfigData.singlePlayerDebugging;
        Values.applyPictureInPictureWorkaround = modConfigData.enablePictureInPictureWorkaround;

        // 注册Payload

        PayloadTypeRegistry.playS2C().register(MorphInitChannelPayload.id, MorphInitChannelPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(MorphVersionChannelPayload.id, MorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MorphCommandPayload.id, MorphCommandPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(LegacyMorphVersionChannelPayload.id, LegacyMorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LegacyMorphCommandPayload.id, LegacyMorphCommandPayload.CODEC);

        if (SharedValues.allowSinglePlayerDebugging)
        {
            // 初始化FeatherMorph服务加载器
            morphServerLoader.onModLoad();

            // 注册指令
            MorphServerLoader.LOGGER.info("Register argument types...");
            MorphServerLoader.LOGGER.info("Sadly, we can't register these at runtime.");

            ArgumentTypeRegistry.registerArgumentType(ResourceLocation.parse("feathermorph:relaxed_string"),
                    RelaxedStringArgumentType.class,
                    SingletonArgumentInfo.contextFree(() -> RelaxedStringArgumentType.INSTANCE));
        }
    }
}
