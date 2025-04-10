package xyz.nifeather.morph.client.graphics.toasts;

import org.joml.Vector2f;
import org.joml.Vector3f;
import xyz.nifeather.morph.client.graphics.Anchor;
import xyz.nifeather.morph.client.graphics.EntityDisplay;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;
import xiamomc.pluginbase.Annotations.Initializer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.network.chat.Component;

public class DisguiseEntryToast extends LinedToast
{
    private final String rawIdentifier;

    private final boolean isGrant;

    public static final ConcurrentLinkedQueue<DisguiseEntryToast> instances = new ConcurrentLinkedQueue<>();

    public static void invalidateAll()
    {
        instances.forEach(DisguiseEntryToast::invalidate);
    }

    private final AtomicBoolean isValid = new AtomicBoolean(true);

    public void invalidate()
    {
        isValid.set(false);
        instances.remove(this);
    }

    public DisguiseEntryToast(String rawIdentifier, boolean isGrant)
    {
        this.rawIdentifier = rawIdentifier;
        this.isGrant = isGrant;

        this.entityDisplay = new ToastEntityDisplay(rawIdentifier, true, EntityDisplay.InitialSetupMethod.NONE);
        entityDisplay.setX(512);
        entityDisplay.setSize(new UVPair(26, 20));
        entityDisplay.setMasking(true);

        entityDisplay.postEntitySetup = () -> setDescription(entityDisplay.getDisplayName());

        instances.add(this);
    }

    @Override
    protected boolean fadeInOnEnter()
    {
        return true;
    }

    @Initializer
    private void load()
    {
        var x = 4;
        var y = 0;

        switch (rawIdentifier)
        {
            case "minecraft:horse" -> {
                x -= 1;
                y += 2;
            }
            case "minecraft:axolotl" -> x -= 1;
            case "minecraft:armor_stand" -> y -= 1;
        }

        entityDisplay.setX(x);
        entityDisplay.setY(y);
        entityDisplay.setAnchor(Anchor.CentreLeft);
        entityDisplay.setParentScreenSpace(new ScreenRectangle(0, 0, this.width(), this.height()));

        setTitle(Component.translatable("text.morphclient.toast.disguise_%s".formatted(isGrant ? "grant" : "lost")));
        this.setLineColor(isGrant ? MaterialColors.Green500 : MaterialColors.Amber500);

        visibility.onValueChanged((o, n) ->
        {
            var isHide = n == Visibility.HIDE;
            if (isHide)
                instances.remove(this);
        }, true);
    }


    private final EntityDisplay entityDisplay;

    @Override
    protected void postBackgroundDrawing(GuiGraphics context, long startTime)
    {
        var matrices = context.pose();
        super.postBackgroundDrawing(context, startTime);

        // Push a new entry to allow us to do some tricks
        matrices.pushMatrix();

        // Draw entity
        // Make entity display more pixel-perfect
        matrices.translate(0, 0.5f, matrices);
        //var pos = matrices.last().pose().getTranslation(new Vector3f(0, 0, 0));

        //todo: IMplement this
        Vector2f pos = new Vector2f(0, 0);

        int mX = (int)pos.x() - 30;
        int mY = (int)pos.y() + (this.height() / 2);

        entityDisplay.setParentScreenSpaceX(pos.x);
        entityDisplay.setParentScreenSpaceY(pos.y);
        entityDisplay.render(context, mX, mY, 0);

        // Pop back
        matrices.popMatrix();
    }

    @Override
    public void update(ToastManager manager, long startTime)
    {
        super.update(manager, startTime);

        this.visibility.set(isValid.get() ? this.visibility.get() : Visibility.HIDE);
    }

    private static final class ToastEntityDisplay extends EntityDisplay
    {
        public ToastEntityDisplay(String rawIdentifier, boolean displayLoadingIfNotValid, InitialSetupMethod initialSetupMethod)
        {
            super(rawIdentifier, displayLoadingIfNotValid, initialSetupMethod);
        }

        public ToastEntityDisplay(String id)
        {
            super(id);
        }

        // workaround: MDrawable中如果此Drawable没有父级，那么会自动通过窗口确定位置
        //             然后无视之前设定的ParentScreenSpace
        @Override
        protected void updateParentScreenSpace()
        {
        }
    }
}
