package xyz.nifeather.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.EntityCache;
import xyz.nifeather.morph.client.FeatherMorphClientBootstrap;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityDisplay extends MDrawable
{
    private final String rawIdentifier;

    private final boolean isPlayerItSelf;

    private final boolean displayLoadingIfInvalid;

    /**
     * 此实体显示初始化的方式
     */
    public enum InitialSetupMethod
    {
        /**
         * 无初始化方式，实体会在第一次渲染时异步设置
         */
        NONE,

        /**
         * 异步设置实体
         */
        ASYNC,

        /**
         * 立即设置实体
         */
        SYNC
    }

    public EntityDisplay(@NotNull String rawIdentifier, boolean displayLoadingIfNotValid, InitialSetupMethod initialSetupMethod)
    {
        this.rawIdentifier = rawIdentifier;
        this.isPlayerItSelf = rawIdentifier.equals(FeatherMorphClientBootstrap.UNMORPH_STIRNG);

        this.displayName = Component.translatable("gui.morphclient.loading")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);

        this.displayLoadingIfInvalid = displayLoadingIfNotValid;

        loadingSpinner.setAnchor(Anchor.Centre);
        loadingSpinner.setParent(this);
        loadingSpinner.setRelativeSizeAxes(Axes.Both);
        loadingSpinner.setSize(new UVPair(1, 1));

        switch (initialSetupMethod)
        {
            case ASYNC -> CompletableFuture.runAsync(this::setupEntity);
            case SYNC -> this.setupEntity();
            case NONE -> { /* 交给load方法 */ }
        }
    }

    public EntityDisplay(String id)
    {
        this(id, false, InitialSetupMethod.NONE);
    }

    @Override
    public void invalidatePosition()
    {
        super.invalidatePosition();
        loadingSpinner.invalidatePosition();
    }

    @Nullable
    private LivingEntity displayingEntity;

    @Nullable
    public LivingEntity getDisplayingEntity()
    {
        return displayingEntity;
    }

    private AtomicBoolean isLiving = new AtomicBoolean(true);

    public boolean isLiving()
    {
        return isLiving.get();
    }

    private Component displayName;

    public Component getDisplayName()
    {
        return displayName;
    }

    private final AtomicInteger initialEntitySize = new AtomicInteger(1);
    private int entityYOffset;

    private int getEntityYOffset(LivingEntity entity)
    {
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return switch (type.toString())
        {
            case "minecraft:ender_dragon" -> -1;
            default -> 0;
        };
    }

    protected int getInitialEntitySize(LivingEntity entity)
    {
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return switch (type.toString())
        {
            case "minecraft:ender_dragon" -> 3;
            case "minecraft:squid", "minecraft:glow_squid" -> 10;
            case "minecraft:horse", "minecraft:player" -> 8;
            default -> 1;
        };
    }

    public void resetEntity()
    {
        this.displayingEntity = null;
    }

    public void doSetupImmedately()
    {
        setupEntity();
    }

    private void setupEntity()
    {
        try
        {
            loadingEntity.set(true);

            var entityCache = EntityCache.getGlobalCache();
            var living = entityCache.getEntity(rawIdentifier, null);
            isLiving.set(entityCache.isLiving(rawIdentifier));

            if (living == null)
            {
                LivingEntity entity = null;

                if (isPlayerItSelf)
                {
                    entity = Minecraft.getInstance().player;
                    isLiving.set(true);
                }

                //没有和此ID匹配的实体
                if (entity == null)
                {
                    Runnable complete = () ->
                    {
                        this.displayName = Component.literal(rawIdentifier);

                        if (postEntitySetup != null)
                            postEntitySetup.run();

                        loadingEntity.set(false);
                    };

                    if (RenderSystem.isOnRenderThread())
                        complete.run();
                    else
                        this.addSchedule(complete);

                    return;
                }

                living = entity;
            }

            LivingEntity finalLiving = living;
            Runnable onComplete = () ->
            {
                loadingEntity.set(false);

                allowRender = true;

                this.displayingEntity = finalLiving;
                this.displayName = finalLiving.getDisplayName();

                initialEntitySize.set(getInitialEntitySize(finalLiving));
                entityYOffset = getEntityYOffset(finalLiving);

                if (postEntitySetup != null)
                    postEntitySetup.run();
            };

            if (RenderSystem.isOnRenderThread())
                onComplete.run();
            else
                this.addSchedule(onComplete);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private final AtomicBoolean loadingEntity = new AtomicBoolean(false);

    public Runnable postEntitySetup;

    private boolean allowRender;

    private final LoadingSpinner loadingSpinner = new LoadingSpinner();

    private void renderLoading(GuiGraphics context)
    {
        loadingSpinner.render(context, 0, 0, 0);
    }

    protected float getRenderScale()
    {
        float scaledMaxEntityBorder = Math.max(displayingEntity.getBbWidth(), displayingEntity.getBbHeight()) * initialEntitySize.get();

        var scale = Math.round((Math.min(this.getRenderHeight(), this.getRenderWidth()) * 0.8f) / scaledMaxEntityBorder);
        scale = Math.max(1, scale);

        return scale;
    }

    @Override
    protected void onRender(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        if (displayingEntity == null && isLiving())
        {
            if (!loadingEntity.get())
                CompletableFuture.runAsync(this::setupEntity);

            renderLoading(context);
            return;
        }

        if (!allowRender || !isLiving())
        {
            if (displayLoadingIfInvalid)
                renderLoading(context);

            return;
        }

        try
        {
            if (displayingEntity.isRemoved())
            {
                resetEntity();
                return;
            }

            //context.fill(0, 0, renderWidth, renderHeight, MaterialColors.Red500.getColor());

            var scale = 1;

            float scaledMaxEntityBorder = Math.max(displayingEntity.getBbWidth(), displayingEntity.getBbHeight()) * initialEntitySize.get();

            scale = Math.round((Math.min(this.getRenderHeight(), this.getRenderWidth()) * 0.8f) / scaledMaxEntityBorder);
            scale = Math.max(1, scale);

            var xStart = (int)getScreenSpaceX();
            var xEnd = xStart + renderWidth;
            var yStart = (int)getScreenSpaceY();
            var yEnd = yStart + renderHeight;

            //context.fill(0, 0, renderWidth, renderHeight, MaterialColors.Teal500.getColor());
            //context.drawString(Minecraft.getInstance().font, "xS: %s, xE: %s, yS: %s, yE: %s".formatted(xStart, xEnd, yStart, yEnd), 0, 0, 0xFFFFFFFF);

            context.pose().translate(-xStart, -yStart);

            PlayerRenderHelper.instance().skipRender = true;

            InventoryScreen.renderEntityInInventoryFollowsMouse(context,
                    xStart, yStart, xEnd, yEnd,
                    scale * initialEntitySize.get(),
                    0.0625f + entityYOffset,
                    (float)mouseX, mouseY,
                    displayingEntity);

            context.pose().translate(xStart, yStart);

            PlayerRenderHelper.instance().skipRender = false;
        }
        catch (Throwable t)
        {
            allowRender = false;
            LoggerFactory.getLogger("morph").error(t.getMessage());
            t.printStackTrace();
        }
    }
}
