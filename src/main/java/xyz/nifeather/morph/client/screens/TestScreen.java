package xyz.nifeather.morph.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class TestScreen extends FeatherScreen
{
    public TestScreen()
    {
        super(Component.literal("screen"));

        entityOne = EntityType.SALMON.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
        entityTwo = EntityType.ARMOR_STAND.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
        entityThree = EntityType.BEE.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
    }

    private LivingEntity entityOne;
    private LivingEntity entityTwo;
    private LivingEntity entityThree;

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f)
    {
        guiGraphics.fill(0, 0, 20, 20, 0xFFABCDEF);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                0, 0, 20, 20,
                20, 0,
                0, 0,
                entityOne
        );

        guiGraphics.fill(40, 40, 80, 80, 0xFFEFDCAB);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                40, 40, 80, 80,
                20, 0,
                0, 0,
                entityTwo
        );

        guiGraphics.fill(100, 100, 200, 200, 0xFFABEFDC);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                100, 100, 200, 200,
                80, 0,
                0, 0,
                entityThree
        );

        super.render(guiGraphics, i, j, f);
    }

    @Override
    public void onClose()
    {
        entityOne.discard();
        entityTwo.discard();
        entityThree.discard();

        super.onClose();
    }
}
