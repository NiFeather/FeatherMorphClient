package xiamo.morph.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.LoggerFactory;
import xiamo.morph.client.bindables.Bindable;
import xiamo.morph.client.mixin.accessors.EntityAccessor;

public class DisguiseSyncer
{
    public DisguiseSyncer()
    {
        MorphClient.selfViewIdentifier.onValueChanged((o, n) -> this.refreshClientViewEntity(n));

        MorphClient.currentNbtCompound.onValueChanged((o, n) ->
        {
            if (n != null) MorphClient.getInstance().schedule(c -> this.mergeNbt(n));
        });

        ClientTickEvents.END_WORLD_TICK.register((w) ->
        {
            if (w != prevWorld && MorphClient.serverReady.get() && prevWorld != null)
                refreshClientViewEntity(MorphClient.selfViewIdentifier.get());

            prevWorld = w;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            prevWorld = null;
        });
    }

    public static Bindable<LivingEntity> currentEntity = new Bindable<>();

    private World prevWorld;

    private void refreshClientViewEntity(String newIdentifier)
    {
        var clientWorld = MinecraftClient.getInstance().world;
        if (clientWorld == null)
        {
            entity = null;
            return;
        }

        var prevEntity = entity;
        var client = MorphClient.getInstance();

        if (prevEntity != null)
        {
            prevEntity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            prevEntity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

            prevEntity.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            prevEntity.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            prevEntity.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
            prevEntity.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);

            client.schedule(c ->
            {
                prevEntity.setRemoved(Entity.RemovalReason.DISCARDED);
                prevEntity.onRemoved();
            });
        }

        entity = EntityCache.getEntity(newIdentifier);
        currentEntity.set(entity);

        allowTick = true;

        if (entity != null)
        {
            client.schedule(c -> clientWorld.addEntity(entity.getId(), entity));

            var nbt = MorphClient.currentNbtCompound.get();
            if (nbt != null)
                client.schedule(c -> mergeNbt(nbt));
        }
    }

    public LivingEntity entity;

    private boolean allowTick = true;

    public void onGameTick()
    {
        if (!allowTick) return;

        try
        {
            var clientPlayer = MinecraftClient.getInstance().player;
            assert clientPlayer != null;

            if (entity != null)
                sync(entity, clientPlayer);
        }
        catch (Exception e)
        {
            onTickError(e);
        }
    }

    public void onGameRender()
    {
        if (!allowTick) return;

        var clientPlayer = MinecraftClient.getInstance().player;

        try
        {
            syncDraw(entity, clientPlayer);
        }
        catch (Exception e)
        {
            onTickError(e);
        }
    }

    private void onTickError(Exception e)
    {
        allowTick = false;
        e.printStackTrace();

        if (entity != null)
        {
            try
            {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
            catch (Exception ee)
            {
                LoggerFactory.getLogger("MorphClient").error("无法移除实体：" + ee.getMessage());
                ee.printStackTrace();
            }

            entity = null;
        }

        var clientPlayer = MinecraftClient.getInstance().player;
        assert clientPlayer != null;

        MorphClient.getInstance().updateClientView(true, false);
        MorphClient.selfViewIdentifier.set(null);

        clientPlayer.sendMessage(Text.literal("更新当前实体时出现错误。"));
        clientPlayer.sendMessage(Text.literal("在当前伪装变更前客户端预览将被禁用以避免游戏崩溃。"));
    }

    private void mergeNbt(NbtCompound nbtCompound)
    {
        if (entity != null)
            entity.readCustomDataFromNbt(nbtCompound);
    }

    private boolean requireTick(EntityType<?> t)
    {
        return t == EntityType.PLAYER;
    }

    private void syncDraw(LivingEntity entity, PlayerEntity clientPlayer)
    {
        if (entity == null || clientPlayer == null) return;

        //幻翼的pitch需要倒转
        if (entity.getType().equals(EntityType.PHANTOM))
            entity.setPitch(-clientPlayer.getPitch());
        else
            entity.setPitch(clientPlayer.getPitch());

        entity.prevPitch = clientPlayer.prevPitch;
    }

    private Vec3d lastPosition = new Vec3d(0, 0, 0);

    private void sync(LivingEntity entity, PlayerEntity clientPlayer)
    {
        var playerPos = clientPlayer.getPos();
        entity.setPosition(playerPos.x, playerPos.y - 4096, playerPos.z);

        if (requireTick(entity.getType()))
        {
            //entity.age++;
            entity.tick();
        }

        if (entity instanceof CamelEntity camelEntity)
        {
            var playerHasVehicle = clientPlayer.hasVehicle();
            var playerStanding = Vec3dUtils.horizontalSquaredDistance(lastPosition, clientPlayer.getPos()) == 0d;

            if (!playerStanding)
                lastPosition = clientPlayer.getPos();

            camelEntity.dashingAnimationState.setRunning(clientPlayer.isSprinting(), camelEntity.age);
            camelEntity.sittingAnimationState.setRunning(playerHasVehicle, camelEntity.age);

            camelEntity.walkingAnimationState.setRunning(!playerHasVehicle && !playerStanding, camelEntity.age);
            camelEntity.idlingAnimationState.setRunning(playerStanding, camelEntity.age);
        }

        var sleepPos = clientPlayer.getSleepingPosition().orElse(null);

        if (sleepPos != null)
            entity.setSleepingPosition(sleepPos);
        else
            entity.clearSleepingPosition();

        entity.headYaw = clientPlayer.headYaw;
        entity.prevHeadYaw = clientPlayer.prevHeadYaw;

        entity.handSwinging = clientPlayer.handSwinging;
        entity.handSwingProgress = clientPlayer.handSwingProgress;
        entity.lastHandSwingProgress = clientPlayer.lastHandSwingProgress;
        entity.handSwingTicks = clientPlayer.handSwingTicks;

        entity.preferredHand = clientPlayer.preferredHand;

        if (entity.getType().equals(EntityType.ARMOR_STAND))
        {
            entity.bodyYaw = clientPlayer.headYaw;
            entity.prevBodyYaw = clientPlayer.prevHeadYaw;
        }
        else
        {
            entity.bodyYaw = clientPlayer.bodyYaw;
            entity.prevBodyYaw = clientPlayer.prevBodyYaw;
        }

        entity.limbAngle = clientPlayer.limbAngle;
        entity.limbDistance = clientPlayer.limbDistance;
        entity.lastLimbDistance = clientPlayer.lastLimbDistance;

        entity.setSneaking(clientPlayer.isSneaking());

        entity.hurtTime = clientPlayer.hurtTime;
        entity.deathTime = clientPlayer.deathTime;

        //entity.inPowderSnow = clientPlayer.inPowderSnow;
        entity.setFrozenTicks(clientPlayer.getFrozenTicks());

        //末影龙的Yaw和玩家是反的
        if (entity.getType().equals(EntityType.ENDER_DRAGON))
            entity.setYaw(180 + clientPlayer.getYaw());

        entity.setOnGround(clientPlayer.isOnGround());

        ((EntityAccessor) entity).setTouchingWater(clientPlayer.isTouchingWater());

        //同步装备
        if (!MorphClient.equipOverriden.get())
        {
            entity.equipStack(EquipmentSlot.MAINHAND, clientPlayer.getEquippedStack(EquipmentSlot.MAINHAND));
            entity.equipStack(EquipmentSlot.OFFHAND, clientPlayer.getEquippedStack(EquipmentSlot.OFFHAND));

            entity.equipStack(EquipmentSlot.HEAD, clientPlayer.getEquippedStack(EquipmentSlot.HEAD));
            entity.equipStack(EquipmentSlot.CHEST, clientPlayer.getEquippedStack(EquipmentSlot.CHEST));
            entity.equipStack(EquipmentSlot.LEGS, clientPlayer.getEquippedStack(EquipmentSlot.LEGS));
            entity.equipStack(EquipmentSlot.FEET, clientPlayer.getEquippedStack(EquipmentSlot.FEET));
        }
        else
        {
            var client = MorphClient.getInstance();

            entity.equipStack(EquipmentSlot.MAINHAND, client.getOverridedItemStackOn(EquipmentSlot.MAINHAND));
            entity.equipStack(EquipmentSlot.OFFHAND, client.getOverridedItemStackOn(EquipmentSlot.OFFHAND));

            entity.equipStack(EquipmentSlot.HEAD, client.getOverridedItemStackOn(EquipmentSlot.HEAD));
            entity.equipStack(EquipmentSlot.CHEST, client.getOverridedItemStackOn(EquipmentSlot.CHEST));
            entity.equipStack(EquipmentSlot.LEGS, client.getOverridedItemStackOn(EquipmentSlot.LEGS));
            entity.equipStack(EquipmentSlot.FEET, client.getOverridedItemStackOn(EquipmentSlot.FEET));
        }

        //同步Pose
        entity.setPose(clientPlayer.getPose());
        entity.setSwimming(clientPlayer.isSwimming());

        if (clientPlayer.hasVehicle())
            entity.startRiding(clientPlayer);
        else if (entity.hasVehicle())
            entity.stopRiding();

        entity.setStuckArrowCount(clientPlayer.getStuckArrowCount());

        if (entity instanceof MorphLocalPlayer player)
        {
            player.fallFlying = clientPlayer.isFallFlying();
            player.usingRiptide = clientPlayer.isUsingRiptide();

            player.fishHook = clientPlayer.fishHook;

            player.itemUseTimeLeft = clientPlayer.getItemUseTimeLeft();
            player.itemUseTime = clientPlayer.getItemUseTime();
            player.setActiveItem(clientPlayer.getActiveItem());

            player.setMainArm(clientPlayer.getMainArm());
        }

        entity.setInvisible(clientPlayer.isInvisible());
    }
}
