package xyz.nifeather.pipfix.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.nifeather.pipfix.TextureTracker;
import xyz.nifeather.pipfix.Values;

@Mixin(PictureInPictureRenderer.class)
public abstract class PictureInPictureRendererMixin<T extends PictureInPictureRenderState>
{
    @Shadow private @Nullable GpuTexture texture;

    @Shadow private @Nullable GpuTextureView textureView;

    @Shadow private @Nullable GpuTexture depthTexture;

    @Shadow private @Nullable GpuTextureView depthTextureView;

    @Shadow protected abstract String getTextureLabel();

    @Shadow @Final private CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer;

    @WrapMethod(method = "prepareTexturesAndProjection")
    public void fmc$prepareTexture(int width, int height, Operation<Void> original)
    {
        if (!Values.applyPictureInPictureWorkaround)
        {
            original.call(width, height);
            return;
        }

        GpuDevice gpuDevice = RenderSystem.getDevice();

        String textureName = "UI " + this.getTextureLabel() + " texture";
        String depthTextureName = "UI " + this.getTextureLabel() + " depth texture";

        this.texture = gpuDevice.createTexture(() -> textureName, 12, TextureFormat.RGBA8, width, height, 1, 1);
        this.texture.setTextureFilter(FilterMode.NEAREST, false);
        this.textureView = gpuDevice.createTextureView(this.texture);

        this.depthTexture = gpuDevice.createTexture(() -> depthTextureName, 8, TextureFormat.DEPTH32, width, height, 1, 1);
        this.depthTextureView = gpuDevice.createTextureView(this.depthTexture);

        TextureTracker.addTrackingTexture(this.texture, this.textureView);
        TextureTracker.addTrackingTexture(this.depthTexture, this.depthTextureView);

        gpuDevice.createCommandEncoder().clearColorAndDepthTextures(this.texture, 0, this.depthTexture, 1.0);
        RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(width, height), ProjectionType.ORTHOGRAPHIC);
    }
}
