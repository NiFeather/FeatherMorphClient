package xyz.nifeather.guiworkaround.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.nifeather.guiworkaround.GuiWorkaroundValues;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>
{
    @Shadow
    public abstract S createRenderState();

    @WrapMethod(
            method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
    )
    private S feathermorph_guifix$forceNewRenderState(Entity entity, float f, Operation<S> original)
    {
        if (!GuiWorkaroundValues.forceNewRenderState)
            return original.call(entity, f);

        S entityRenderState = this.createRenderState();
        ((EntityRenderer)(Object)this).extractRenderState(entity, entityRenderState, f);
        return entityRenderState;
    }
}
