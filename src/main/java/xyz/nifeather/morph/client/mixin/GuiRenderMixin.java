package xyz.nifeather.morph.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.graphics.TextureTracker;
import xyz.nifeather.morph.shared.SharedValues;

@Mixin(GuiRenderer.class)
public class GuiRenderMixin
{
    @Inject(method = "render", at = @At("TAIL"))
    public void fmc$postRender(GpuBufferSlice gpuBufferSlice, CallbackInfo ci)
    {
        if (SharedValues.applyPictureInPictureWorkaround)
            TextureTracker.disposeTextures();
    }
}
