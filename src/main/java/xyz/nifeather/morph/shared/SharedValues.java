package xyz.nifeather.morph.shared;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.mixin.PictureInPictureRendererMixin;

public class SharedValues
{
    public static boolean allowSinglePlayerDebugging = false;
    public static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph");

    private static final String morphNameSpace = "morphplugin";
    public static final String MOD_ID = "feathermorph-client";

    public static ResourceLocation initializeChannelIdentifier = ResourceLocation.fromNamespaceAndPath(morphNameSpace, "init");
    public static ResourceLocation versionChannelIdentifier = ResourceLocation.fromNamespaceAndPath(morphNameSpace, "version_v2");
    public static ResourceLocation commandChannelIdentifier = ResourceLocation.fromNamespaceAndPath(morphNameSpace, "commands_v2");

    public static final String newProtocolIdentify = "1_21_3_packetbuf";

    public static ResourceLocation versionChannelIdentifierLegacy = ResourceLocation.fromNamespaceAndPath(morphNameSpace, "version");
    public static ResourceLocation commandChannelIdentifierLegacy = ResourceLocation.fromNamespaceAndPath(morphNameSpace, "commands");

    public static boolean client_UseNewPacketSerializeMethod = false;

    /**
     * See {@link PictureInPictureRendererMixin} and {@link xyz.nifeather.morph.client.mixin.GuiRenderMixin}
     * If set to false, these mixins will not operate
     */
    public static boolean applyPictureInPictureWorkaround = true;
}
