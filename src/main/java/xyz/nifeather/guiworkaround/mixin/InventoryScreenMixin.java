package xyz.nifeather.guiworkaround.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import xyz.nifeather.guiworkaround.GuiWorkaroundValues;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin
{
    @WrapMethod(method = "renderEntityInInventoryFollowsMouse")
    private static void feathermorph_guifix$fixInventoryDraw(GuiGraphics guiGraphics,
                                                             int i, int j, int k, int l,
                                                             int m, float f, float g, float h,
                                                             LivingEntity livingEntity, Operation<Void> original)
    {
        GuiWorkaroundValues.forceNewRenderState = true;
        original.call(guiGraphics, i, j, k, l, m, f, g, h, livingEntity);
        GuiWorkaroundValues.forceNewRenderState = false;
    }
}
