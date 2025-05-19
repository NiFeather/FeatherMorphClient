package xyz.nifeather.pipfix;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TextureTracker
{
    private static final Map<GpuTexture, GpuTextureView> trackingTextures = new ConcurrentHashMap<>();

    public static void addTrackingTexture(GpuTexture texture, @NotNull GpuTextureView textureView)
    {
        synchronized (trackingTextures)
        {
            trackingTextures.put(texture, textureView);
        }
    }

    public static void disposeTextures()
    {
        synchronized (trackingTextures)
        {
            if (trackingTextures.isEmpty())
                return;

            trackingTextures.forEach((tex, view) ->
            {
                tex.close();
                view.close();
            });

            trackingTextures.clear();
        }
    }
}
