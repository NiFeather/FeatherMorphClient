package xyz.nifeather.morph.client.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nifeather.morph.client.ServerHandler;
import xyz.nifeather.morph.client.entities.IMorphClientPlayer;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin implements IMorphClientPlayer
{
    @Shadow
    @Nullable
    public Input input;

    @Shadow private boolean lastSneaking;
    @Nullable
    private Boolean inputLastValue;

    @Nullable
    @Unique
    private Boolean morphclient$serverSneaking;

    @Override
    public void morphclient$setServerSneaking(boolean sneaking)
    {
        morphclient$serverSneaking = sneaking;
        this.lastSneaking = sneaking;
    }
/*
    @Inject(method = "sendSneakingPacket",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
    private void aa(CallbackInfo ci)
    {
        FeatherMorphClient.LOGGER.info("SendSneaking!");
    }
*/
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void onSneakingCall(CallbackInfoReturnable<Boolean> cir)
    {
        var serverSideSneaking = morphclient$serverSneaking;

        //如果input的下蹲状态发生变化，则重置服务器状态并返回input的当前状态
        if (input != null && (inputLastValue == null || input.playerInput.sneak() != inputLastValue))
        {
            inputLastValue = input.playerInput.sneak();

            cir.setReturnValue(input.playerInput.sneak());
            morphclient$serverSneaking = serverSideSneaking = null;
            return;
        }

        //否则返回服务器状态
        if (serverSideSneaking != null)
            cir.setReturnValue(serverSideSneaking);
    }
}
