package xiamomc.morph.client.mixin;

import net.minecraft.entity.passive.FoxEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xiamomc.morph.client.entities.IFox;

@Mixin(FoxEntity.class)
public abstract class FoxMixin implements IFox
{
    @Shadow abstract void setSleeping(boolean sleeping);

    @Override
    public void morphclient$forceSetSleeping(boolean sleeping)
    {
        this.setSleeping(sleeping);
    }
}