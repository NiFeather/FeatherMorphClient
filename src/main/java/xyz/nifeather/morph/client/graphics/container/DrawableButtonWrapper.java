package xyz.nifeather.morph.client.graphics.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import xyz.nifeather.morph.client.graphics.MDrawable;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;

public class DrawableButtonWrapper extends MDrawable
{
    private final Button widget;

    public DrawableButtonWrapper(Button widget)
    {
        this.setHeight(widget.getHeight());
        this.setWidth(widget.getWidth());
        this.widget = widget;
    }

    @Override
    protected void updatePosition()
    {
        super.updatePosition();

        widget.setX(Math.round(this.getScreenSpaceX()));
        widget.setY(Math.round(this.getScreenSpaceY()));
    }

    @Override
    protected void onRender(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        context.pose().translate(-this.getScreenSpaceX(), -this.getScreenSpaceY(), context.pose());
        widget.render(context, mouseX, mouseY, delta);

        super.onRender(context, mouseX, mouseY, delta);
    }

    @Override
    public void setFocused(boolean focused)
    {
        widget.setFocused(focused);
    }

    @Override
    public boolean isFocused()
    {
        return widget.isFocused();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        return widget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        return widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        widget.mouseClicked(mouseX, mouseY, button);
        return true;
    }
}
