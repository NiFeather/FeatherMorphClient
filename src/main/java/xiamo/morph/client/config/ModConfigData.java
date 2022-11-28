package xiamo.morph.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import xiamo.morph.client.MorphClient;

@Config(name = "morphclient")
public class ModConfigData implements ConfigData
{
    public boolean alwaysShowPreviewInInventory = false;

    public boolean allowClientView = false;

    public boolean clientViewVisible()
    {
        return MorphClient.getInstance().selfVisibleToggled.get() && allowClientView;
    }
}