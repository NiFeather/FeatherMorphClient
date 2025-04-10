package xyz.nifeather.morph.client.graphics.container;

import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.client.graphics.MDrawable;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;

import java.util.function.Consumer;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public class TextFieldWidgetWrapper extends MDrawable
{
    public EditBox widget;

    public TextFieldWidgetWrapper(EditBox fieldWidget)
    {
        this.widget = fieldWidget;

        this.setHeight(fieldWidget.getHeight());
        this.setWidth(fieldWidget.getWidth());
    }

    public EditBox widget()
    {
        return widget;
    }

    public void setChangedListener(Consumer<String> changedListener)
    {
        widget.setResponder(changedListener);
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
    public void setWidth(float w)
    {
        super.setWidth(w);

        widget.setWidth(Math.round(w));
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation)
    {
        return widget.nextFocusPath(navigation);
    }

    @Override
    public ScreenRectangle getRectangle()
    {
        return widget.getRectangle();
    }

    @Override
    public @Nullable ComponentPath getCurrentFocusPath()
    {
        return widget.getCurrentFocusPath();
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
    public void setHeight(float h)
    {
        super.setHeight(h);

        widget.setHeight(Math.round(h));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        return widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        return widget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers)
    {
        return widget.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        logger.info("CLICKL! is Over? " + widget.isMouseOver(mouseX, mouseY));
        return widget.mouseClicked(mouseX, mouseY, button);
    }
}
