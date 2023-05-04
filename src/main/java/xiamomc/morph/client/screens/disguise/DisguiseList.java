package xiamomc.morph.client.screens.disguise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;

public class DisguiseList extends ElementListWidget<EntityDisplayWidget>
{
    public DisguiseList(MinecraftClient minecraftClient, int width, int height, int topPadding, int bottomPadding, int itemHeight)
    {
        super(minecraftClient, width, height, topPadding, bottomPadding, itemHeight);
    }

    public void clearChildren()
    {
        children().forEach(EntityDisplayWidget::clearChildren);
        clearEntries();
    }

    public void setHeight(int nH)
    {
        this.height = nH;
    }

    public void setWidth(int w)
    {
        this.width = w;
    }

    public void setBottomPadding(int b)
    {
        this.bottom = b;
    }

    public int getBottomPadding()
    {
        return this.bottom;
    }

    public int getTopPadding()
    {
        return this.top;
    }

    public void setTopPadding(int newPadding)
    {
        this.top = newPadding;
    }

    public void scrollTo(EntityDisplayWidget widget)
    {
        if (widget == null || !children().contains(widget)) return;

        //top和bottom此时可能正处于动画中，因此需要我们自己确定最终屏幕的可用空间大小
        //在界面Header和Footer替换成我们自己的实现之前先这样
        var fontMargin = 4;
        var topPadding = MinecraftClient.getInstance().textRenderer.fontHeight * 2 + fontMargin * 2;
        var bottomPadding = 30;
        var finalScreenSpaceHeight = this.height - topPadding - bottomPadding;

        var amount = children().indexOf(widget) * itemHeight - itemHeight * 3;
        var maxScroll = this.getEntryCount() * this.itemHeight - finalScreenSpaceHeight + 4;
        if (amount > maxScroll) amount = maxScroll;

        this.setScrollAmount(amount);
    }

    @Override
    public int getRowWidth()
    {
        return 200;
    }

    public void setHeaderHeight(int newHeaderHeight)
    {
        this.setRenderHeader(true, newHeaderHeight);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        this.setRenderBackground(false);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBackground(MatrixStack matrices)
    {
    }
}
