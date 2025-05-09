package xyz.nifeather.morph.client.syncers;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.*;
import xyz.nifeather.morph.client.entities.IDisguiseRenderState;
import xyz.nifeather.morph.client.entities.IMorphClientEntity;
import xyz.nifeather.morph.client.entities.MorphLocalPlayer;
import xyz.nifeather.morph.client.mixin.accessors.AbstractHorseEntityMixin;
import xyz.nifeather.morph.client.mixin.accessors.EntityAccessor;
import xyz.nifeather.morph.client.mixin.accessors.LimbAnimatorAccessor;
import xyz.nifeather.morph.client.syncers.animations.AnimationHandler;
import xiamomc.pluginbase.Annotations.Resolved;
import xyz.nifeather.morph.client.utilties.ClientItemUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DisguiseSyncer extends MorphClientObject
{
    @Nullable
    protected LivingEntity disguiseInstance;

    @Nullable
    public LivingEntity getDisguiseInstance()
    {
        return disguiseInstance;
    }

    @NotNull
    protected AbstractClientPlayer bindingPlayer;

    @NotNull
    private ConvertedMeta bindingMeta = new ConvertedMeta();

    @NotNull
    protected ConvertedMeta getBindingMeta()
    {
        return bindingMeta;
    }

    @Nullable
    protected CompoundTag getCompound()
    {
        return bindingMeta.nbt;
    }

    protected final String disguiseId;

    protected final int bindingNetworkId;

    @Resolved(shouldSolveImmediately = true)
    private DisguiseInstanceTracker instanceTracker;

    @NotNull
    protected abstract EntityCache getEntityCache();

    @NotNull
    public AbstractClientPlayer getBindingPlayer()
    {
        return bindingPlayer;
    }

    public DisguiseSyncer(@NotNull AbstractClientPlayer bindingPlayer, String morphId, int networkId)
    {
        this.bindingPlayer = bindingPlayer;
        this.disguiseId = morphId;
        this.bindingNetworkId = networkId;

        bindingMeta.outdated = true;

        refreshEntity();
    }

    private AnimationHandler animHandler;

    public void setAnimationHandler(AnimationHandler handler)
    {
        this.animHandler = handler;
    }

    public void playAnimation(String animation)
    {
        if (animHandler == null)
        {
            logger.warn("No animation handler for disguise '%s'!".formatted(disguiseId));
            return;
        }

        animHandler.play(disguiseInstance, animation);
    }

    private int crystalId;

    protected Entity beamTarget;

    private void scheduleCrystalSearch()
    {
        if (beamTarget != null || crystalId == 0) return;

        this.addSchedule(this::scheduleCrystalSearch, 10);

        this.beamTarget = findCrystalBy(crystalId);
    }

    @Nullable
    private Entity findCrystalBy(int id)
    {
        if (disguiseInstance == null || id == 0) return null;

        var world = bindingPlayer.level();
        if (world == null) return null;

        return world.getEntity(id);
    }

    /**
     * @return Whether we created the entity successfully
     */
    public boolean refreshEntity()
    {
        try (var clientWorld = Minecraft.getInstance().level)
        {
            if (clientWorld == null)
            {
                disguiseInstance = null;
                return false;
            }

            var entityCache = getEntityCache();

            var prevEntity = disguiseInstance;
            var client = FeatherMorphClient.getInstance();

            if (prevEntity != null)
            {
                prevEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                prevEntity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

                prevEntity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                prevEntity.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                prevEntity.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                prevEntity.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);

                beamTarget = null;

                prevEntity.hurtTime = 0;

                prevEntity.discard();
                entityCache.discardEntity(disguiseId);
            }

            var newInstance = entityCache.getEntity(disguiseId, bindingPlayer);

            if (newInstance == null)
            {
                logger.info("Can't create entity with ID: %s, is it valid for the client?".formatted(disguiseId));
                return false;
            }

            newInstance.setId(newInstance.getId() - newInstance.getId() * 2);

            client.schedule(() -> clientWorld.addEntity(newInstance));

            var nbt = getCompound();
            if (nbt != null)
                client.schedule(() -> mergeNbt(nbt));

            newInstance.addTag("BINDING_" + bindingPlayer.getId());
            newInstance.noPhysics = true;

            if (newInstance instanceof IMorphClientEntity iMorphEntity)
                iMorphEntity.featherMorph$setIsDisguiseEntity(bindingNetworkId);

            if (newInstance instanceof MorphLocalPlayer localPlayer)
            {
                localPlayer.setBindingPlayer(Minecraft.getInstance().player);

                if (prevEntity instanceof MorphLocalPlayer prevPlayer && prevPlayer.personEquals(localPlayer))
                    localPlayer.copyFrom(prevPlayer);
            }

            this.disguiseInstance = newInstance;

            initialSync();
            baseSync();
        }
        catch (Throwable t)
        {
            logger.error("Error occurred while refreshing client view: %s".formatted(t.getMessage()));
            t.printStackTrace();

            disguiseInstance = null;
            return false;
        }

        return true;
    }

    public void playAttackAnimation()
    {
        if (disguiseInstance != null)
            disguiseInstance.handleEntityEvent(EntityEvent.START_ATTACKING);
    }

    private static final ItemStack air = new ItemStack(Items.AIR);

    protected void mergeNbt(CompoundTag nbtCompound)
    {
        var entity = disguiseInstance;

        if (entity == null) return;

        try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), FeatherMorphClient.LOGGER))
        {
            entity.load(TagValueInput.create(scopedCollector, entity.registryAccess(), nbtCompound));
        }

        if (entity instanceof Horse horse)
        {
            var haveSaddle = nbtCompound.contains("SaddleItem");

            if (haveSaddle)
            {
                ItemStack itemStack = ClientItemUtils.fromCompound(bindingPlayer.level().registryAccess(), nbtCompound.getCompound("SaddleItem").orElseThrow())
                        .orElse(air);

                var isSaddle = itemStack.is(Items.SADDLE);

                ((AbstractHorseEntityMixin) horse).callSetFlag(4, isSaddle);
            }

            //Doesn't work for unknown reason
            if (nbtCompound.contains("ArmorItem"))
            {
                ItemStack armorItem = ClientItemUtils.fromCompound(bindingPlayer.level().registryAccess(), nbtCompound.getCompound("ArmorItem").orElseThrow())
                        .orElse(air);

                horse.setBodyArmorItem(armorItem);
            }
        }

        bindingPlayer.refreshDimensions();

        var crystalPosition = nbtCompound.getInt("BeamTarget").orElse(-1);
        crystalId = crystalPosition;
        this.beamTarget = findCrystalBy(crystalPosition);

        if (beamTarget == null)
            this.scheduleCrystalSearch();
    }

    //region DisguiseSyncing

    private final AtomicBoolean isSyncing = new AtomicBoolean(false);

    protected void markSyncing()
    {
        isSyncing.set(true);
    }

    protected void markNotSyncing()
    {
        isSyncing.set(false);
    }

    public boolean isSyncing()
    {
        return isSyncing.get();
    }

    private boolean allowTick = true;

    protected void onTickError()
    {
    }

    private void onSyncError(Exception e)
    {
        allowTick = false;
        markNotSyncing();

        logger.error(e.getMessage());
        e.printStackTrace();

        if (disguiseInstance != null)
        {
            try
            {
                disguiseInstance.remove(Entity.RemovalReason.DISCARDED);
            }
            catch (Exception ee)
            {
                LoggerFactory.getLogger("MorphClient").error("Unable to remove entity:" + ee.getMessage());
                ee.printStackTrace();
            }

            disguiseInstance = null;
        }

        onTickError();

        var clientPlayer = Minecraft.getInstance().player;
        assert clientPlayer != null;

        clientPlayer.displayClientMessage(Component.literal(this + "Sync Failed!"), false);
    }

    public void onGameTick()
    {
        if (!allowTick) return;

        try
        {
            var clientPlayer = Minecraft.getInstance().player;
            assert clientPlayer != null;

            syncTick();
        }
        catch (Exception e)
        {
            onSyncError(e);
        }
    }

    public void postEntityRender()
    {
    }

    public void onEarlyEntityRender()
    {
        if (!allowTick) return;

        try
        {
            preEntityRender();
        }
        catch (Exception e)
        {
            onSyncError(e);
        }
    }

    public void updateSkin(GameProfile profile)
    {
        if (disposed.get())
        {
            logger.warn("Trying to update skin for a disposed DisguiseSyncer " + this);
            Thread.dumpStack();
            return;
        }

        if (!RenderSystem.isOnRenderThread())
            throw new RuntimeException("May not invoke updateSkin() while not on the render thread.");

        if (disguiseInstance instanceof MorphLocalPlayer localPlayer)
            localPlayer.updateSkin(profile);
        else
            LoggerFactory.getLogger("MorphClient")
                    .warn(this + " Received a GameProfile while current disguise is not a player! Current instance is %s".formatted(disguiseInstance));
    }

    public abstract void syncTick();

    public void onEntityRenderStateSetup(EntityRenderState renderState, IDisguiseRenderState asDisguiseRenderState)
    {
    }

    public abstract void preEntityRender();

    protected abstract void initialSync();

    protected void syncYawPitch()
    {
        if (disguiseInstance == null) return;

        var player = bindingPlayer;

        // 幻翼的pitch需要倒转
        // Don't sync pitch when sleeping position is present -- Match plugin behavior (maybe?)
        if (disguiseInstance.getSleepingPos().isEmpty())
        {
            if (disguiseInstance.getType() == EntityType.PHANTOM)
                disguiseInstance.setXRot(-player.getXRot());
            else
                disguiseInstance.setXRot(player.getXRot());
        }

        //末影龙的Yaw和玩家是反的
        if (disguiseInstance.getType() == EntityType.ENDER_DRAGON)
            disguiseInstance.setYRot(180 + player.getYRot());
        else
            disguiseInstance.setYRot(player.getYRot());

        disguiseInstance.yHeadRot = player.yHeadRot;
        disguiseInstance.yHeadRotO = player.yHeadRotO;

        if (disguiseInstance.getType() == EntityType.ARMOR_STAND)
        {
            disguiseInstance.yBodyRot = player.yHeadRot;
            disguiseInstance.yBodyRotO = player.yHeadRotO;
        }
    }

    protected boolean showOverridedEquips()
    {
        return bindingMeta.showOverridedEquips;
    }

    protected void syncEquipments()
    {
        if (disguiseInstance == null) return;

        var meta = getBindingMeta();
        var showOverridedEquips = showOverridedEquips();
        var disguiseEquip = meta.convertedEquipment;

        // In case the server returned a bad meta...
        if (disguiseEquip == null && showOverridedEquips)
            return;

        var headStack = showOverridedEquips ? disguiseEquip.head : bindingPlayer.getItemBySlot(EquipmentSlot.HEAD);
        var chestStack = showOverridedEquips ? disguiseEquip.chest : bindingPlayer.getItemBySlot(EquipmentSlot.CHEST);
        var legStack = showOverridedEquips ? disguiseEquip.leggings : bindingPlayer.getItemBySlot(EquipmentSlot.LEGS);
        var feetStack = showOverridedEquips ? disguiseEquip.feet : bindingPlayer.getItemBySlot(EquipmentSlot.FEET);
        var handStack = showOverridedEquips ? disguiseEquip.mainHand : bindingPlayer.getItemBySlot(EquipmentSlot.MAINHAND);
        var offHandStack = showOverridedEquips ? disguiseEquip.offHand : bindingPlayer.getItemBySlot(EquipmentSlot.OFFHAND);

        //logger.info("Show disguised? " + showOverridedEquips + " :: Checkstack? " + chestStack);

        disguiseInstance.setItemSlot(EquipmentSlot.MAINHAND, handStack);
        disguiseInstance.setItemSlot(EquipmentSlot.OFFHAND, offHandStack);

        disguiseInstance.setItemSlot(EquipmentSlot.HEAD, headStack);
        disguiseInstance.setItemSlot(EquipmentSlot.CHEST, chestStack);
        disguiseInstance.setItemSlot(EquipmentSlot.LEGS, legStack);
        disguiseInstance.setItemSlot(EquipmentSlot.FEET, feetStack);
    }

    protected abstract void syncPosition();

    private void preMetaChange(ConvertedMeta meta)
    {
        if (meta.nbt != null)
            this.mergeNbt(meta.nbt);

        if (meta.profileNbt != null && this.disguiseInstance instanceof MorphLocalPlayer localPlayer)
            localPlayer.updateSkin(meta.profileNbt);

        meta.outdated = false;

        this.bindingMeta = meta;
    }

    private ClientLevel world;
    private ClientLevel prevWorld;
    protected void baseSync()
    {
        var entity = disguiseInstance;
        if (entity == null) return;

        if (this.disposed())
        {
            logger.warn("Trying to update a disposed DisguiseSyncer(%s)!".formatted(this));
            Thread.dumpStack();
            return;
        }

        if (bindingPlayer.isRemoved() || bindingPlayer.level() != Minecraft.getInstance().level)
        {
            logger.info(this + " Player removed, scheduling dispose");
            this.addSchedule(this::dispose);
            return;
        }

        world = Minecraft.getInstance().level;

        if (world != prevWorld)
        {
            var prev = prevWorld;
            prevWorld = world;

            if (prev != null)
            {
                logger.info(this + " World changed, refreshing");

                getEntityCache().dropAll();
                refreshEntity();
            }

            return;
        }

        if (disguiseInstance.isRemoved())
        {
            logger.info(this + " Instance removed, refreshing");
            refreshEntity();
            return;
        }

        if (bindingMeta.outdated)
            this.preMetaChange(instanceTracker.getMetaFor(this.bindingNetworkId));

        markSyncing();
        syncPosition();
        syncEquipments();

        // 因为我们在LivingEntity和PlayerEntity那里都加了阻止伪装实体被世界tick的mixin,
        // 所以在这里手动调用tick
        entity.tick();

        if (beamTarget != null && beamTarget.isRemoved())
            beamTarget = null;

        var entitylimbAnimatorAccessor = (LimbAnimatorAccessor) entity.walkAnimation;
        var playerLimbAccessor = (LimbAnimatorAccessor) bindingPlayer.walkAnimation;
        var playerLimb = bindingPlayer.walkAnimation;

        entitylimbAnimatorAccessor.setSpeedOld(playerLimbAccessor.getSpeedOld());
        entitylimbAnimatorAccessor.setPosition(playerLimb.position());
        entitylimbAnimatorAccessor.setSpeed(playerLimb.speed());

        // Sleep Pos
        var sleepPos = bindingPlayer.getSleepingPos().orElse(null);

        if (sleepPos != null)
            entity.setSleepingPos(sleepPos);
        else
            entity.clearSleepingPos();

        entity.setSharedFlagOnFire(bindingPlayer.isOnFire());

        // Health
        var healthAttribute = entity.getAttribute(Attributes.MAX_HEALTH);

        if (healthAttribute != null)
            healthAttribute.setBaseValue(bindingPlayer.getMaxHealth());

        entity.setHealth(bindingPlayer.getHealth());

        // Scale
        var scaleAttribute = entity.getAttribute(Attributes.SCALE);

        if (scaleAttribute != null && scaleAttribute.getValue() != bindingPlayer.getAttributeValue(Attributes.SCALE))
            scaleAttribute.setBaseValue(bindingPlayer.getAttributeValue(Attributes.SCALE));

        // Hand Swing
        entity.swinging = bindingPlayer.swinging;
        entity.attackAnim = bindingPlayer.attackAnim;
        entity.oAttackAnim = bindingPlayer.oAttackAnim;
        entity.swingTime = bindingPlayer.swingTime;
        entity.swingingArm = bindingPlayer.swingingArm;

        // Hand and sneaking
        entity.setShiftKeyDown(bindingPlayer.isShiftKeyDown());

        // Hurt and death
        entity.hurtTime = bindingPlayer.hurtTime;
        entity.deathTime = bindingPlayer.deathTime;

        entity.setSprinting(bindingPlayer.isSprinting());

        //entity.inPowderSnow = clientPlayer.inPowderSnow;
        entity.setTicksFrozen(bindingPlayer.getTicksFrozen());

        entity.setDeltaMovement(bindingPlayer.getDeltaMovement());

        entity.setOnGround(bindingPlayer.onGround());

        ((EntityAccessor) entity).setWasTouchingWater(bindingPlayer.isInWater());

        // Glowing
        if (bindingPlayer.isCurrentlyGlowing() != entity.isCurrentlyGlowing())
            entity.setGlowingTag(bindingPlayer.isCurrentlyGlowing());

        //同步Pose
        entity.setPose(bindingPlayer.getPose());
        entity.setSwimming(bindingPlayer.isSwimming());

        entity.isUsingItem();
        entity.releaseUsingItem();

        if (bindingPlayer.isPassenger() && entity.getVehicle() != bindingPlayer)
        {
            if (entity instanceof IMorphClientEntity asMorphClientEntity)
                asMorphClientEntity.featherMorph$overridePose(null);

            entity.startRiding(bindingPlayer, true);
        }
        else if (!bindingPlayer.isPassenger() && entity.isPassenger())
        {
            entity.stopRiding();
        }

        entity.setArrowCount(bindingPlayer.getArrowCount());

        if (entity instanceof MorphLocalPlayer player)
            player.fishing = bindingPlayer.fishing;

        entity.setInvisible(bindingPlayer.isInvisible());

        markNotSyncing();
    }

    //endregion

    //region Disposal

    private final AtomicBoolean disposed = new AtomicBoolean(false);

    public final boolean disposed()
    {
        return disposed.get();
    }

    protected abstract void onDispose();

    public final void dispose()
    {
        if (disposed())
            return;

        if (getEntityCache() != EntityCache.getGlobalCache())
            getEntityCache().dispose();

        if (disguiseInstance != null)
        {
            if (RenderSystem.isOnRenderThread())
                disguiseInstance.discard();
            else
                addSchedule(disguiseInstance::discard);
        }

        try
        {
            this.onDispose();
        }
        catch (Throwable t)
        {
            logger.warn("Error calling onDispose() for DisguiseSyncer: %s".formatted(t.getMessage()));
            t.printStackTrace();
        }

        disguiseInstance = null;
        world = null;
        prevWorld = null;
        disposed.set(true);

        bindingPlayer.refreshDimensions();

        try
        {
            postDispose();
        }
        catch (Throwable t)
        {
            logger.warn("Error calling postDispose() for DisguiseSyncer: %s".formatted(t.getMessage()));
            t.printStackTrace();
        }
    }

    protected void postDispose()
    {
    }

    //endregion Disposal
}
