package xyz.nifeather.morph;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;
import xyz.nifeather.morph.client.config.ModConfigData;
import xyz.nifeather.morph.server.MorphServerLoader;
import xyz.nifeather.morph.shared.SharedValues;
import xyz.nifeather.morph.shared.commands.arguments.RelaxedStringArgumentType;
import xyz.nifeather.morph.shared.payload.V1MorphCommandPayload;
import xyz.nifeather.morph.shared.payload.V1MorphVersionChannelPayload;
import xyz.nifeather.morph.shared.payload.V1V2MorphInitChannelPayload;
import xyz.nifeather.morph.shared.payload.V2MorphCommandPayload;
import xyz.nifeather.morph.shared.payload.V2MorphVersionChannelPayload;
import xyz.nifeather.morph.shared.payload.V3MorphCommandPayload;
import xyz.nifeather.morph.shared.payload.V3MorphInitChannelPayload;
import xyz.nifeather.morph.shared.platform.Services;
import xyz.nifeather.morph.shared.payload.*;

public class FeatherMorphCommonBootstrap
{
    private static FeatherMorphCommonBootstrap INSTANCE;
    private final MorphServerLoader morphServerLoader = new MorphServerLoader();
    public ModConfigData modConfigData;
    public ConfigHolder<ModConfigData> configHolder;

    public FeatherMorphCommonBootstrap()
    {
        INSTANCE = this;

        // 初始化配置
        AutoConfig.register(ModConfigData.class, GsonConfigSerializer::new);

        configHolder = AutoConfig.getConfigHolder(ModConfigData.class);
        configHolder.load();

        modConfigData = configHolder.getConfig();

        SharedValues.allowSinglePlayerDebugging = modConfigData.singlePlayerDebugging;

        // 注册Payload
        PayloadTypeRegistry.playS2C().register(V3MorphInitChannelPayload.id, V3MorphInitChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(V3MorphCommandPayload.id, V3MorphCommandPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(V2MorphVersionChannelPayload.id, V2MorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(V2MorphCommandPayload.id, V2MorphCommandPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(V1MorphVersionChannelPayload.id, V1MorphVersionChannelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(V1MorphCommandPayload.id, V1MorphCommandPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(V1V2MorphInitChannelPayload.id, V1V2MorphInitChannelPayload.CODEC);

        if (SharedValues.allowSinglePlayerDebugging)
        {
            // 初始化FeatherMorph服务加载器
            morphServerLoader.onModLoad();

            // 注册指令
            MorphServerLoader.LOGGER.info("Register argument types...");
            MorphServerLoader.LOGGER.info("Sadly, we can't register these at runtime.");

            Services.PLATFORM.registerArgumentType(ResourceLocation.parse("feathermorph:relaxed_string"),
                    RelaxedStringArgumentType.class,
                    SingletonArgumentInfo.contextFree(() -> RelaxedStringArgumentType.INSTANCE));
        }
    }

    public static FeatherMorphCommonBootstrap instance()
    {
        return INSTANCE;
    }
}
