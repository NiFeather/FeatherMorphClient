package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nifeather.morph.client.entities.IDisguiseRenderState;
import xyz.nifeather.morph.client.graphics.EntityRendererHelper;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin
{
    @Inject(
            method = "extractRenderState",
            at = @At("RETURN")
    )
    public void morphclient$setupDisguiseRenderState(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci)
    {
        if (state instanceof IDisguiseRenderState disguiseRenderState)
            EntityRendererHelper.instance.setupEntityState(entity, disguiseRenderState);
    }
}
