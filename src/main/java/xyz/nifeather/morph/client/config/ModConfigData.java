package xyz.nifeather.morph.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import xyz.nifeather.morph.client.FeatherMorphClient;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;

@Config(name = "morphclient")
public class ModConfigData implements ConfigData
{
    public boolean alwaysShowPreviewInInventory = false;

    public boolean allowClientView = true;

    public boolean verbosePackets = false;

    public boolean displayDisguiseOnHud = true;

    public boolean changeCameraHeight = false;

    public Easing easing = Easing.OutQuint;

    public int duration = 450;

    public float scrollSpeed = 2.5f;

    public boolean scaleNameTag = false;

    public boolean displayGrantRevokeToast = true;
    public boolean displayQuerySetToast = false;
    public boolean displayToastProgress = false;

    public boolean singlePlayerDebugging = false;

    public boolean disguiseListSmoothScroll = true;

    public boolean enablePictureInPictureWorkaround = true;

    public boolean clientViewVisible()
    {
        return FeatherMorphClient.getInstance().morphManager.selfVisibleEnabled.get() && allowClientView;
    }
}
