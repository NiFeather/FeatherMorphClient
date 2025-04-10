package xyz.nifeather.morph.client.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import xyz.nifeather.morph.client.MorphClientObject;
import xyz.nifeather.morph.client.graphics.color.ColorUtils;
import xyz.nifeather.morph.client.graphics.color.MaterialColors;
import xyz.nifeather.morph.client.graphics.transforms.Recorder;
import xyz.nifeather.morph.client.graphics.transforms.Transformer;
import xyz.nifeather.morph.client.graphics.transforms.easings.Easing;
import xyz.nifeather.morph.client.utilties.MathUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.geom.builders.UVPair;

public abstract class MDrawable extends MorphClientObject implements IMDrawable
{
    protected int depth = 0;

    @Override
    public int getDepth()
    {
        return depth;
    }

    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    //region Anchor

    @NotNull
    protected Anchor anchor = Anchor.TopLeft;

    @NotNull
    public Anchor getAnchor()
    {
        return anchor;
    }

    public void setAnchor(@NotNull Anchor anchor)
    {
        if (anchor == this.anchor) return;

        this.anchor = anchor;
        invalidatePosition();
    }

    //endregion Anchor

    //region Parent

    protected IMDrawable parent;

    public void setParent(IMDrawable parent)
    {
        if (this.parent == parent) return;

        if (this.parent != null)
            throw new RuntimeException("A drawable may not have multiple parents.");

        this.parent = parent;
        invalidatePosition();
        invalidateLayout();
    }

    public IMDrawable getParent()
    {
        return parent;
    }

    //endregion Parent

    //region Position/Layout validation

    protected final AtomicBoolean posValid = new AtomicBoolean(false);

    @ApiStatus.Internal
    public void invalidatePosition()
    {
        posValid.set(false);
    }

    protected final AtomicBoolean layoutValid = new AtomicBoolean(false);

    @ApiStatus.Internal
    public void invalidateLayout()
    {
        layoutValid.set(false);
    }

    protected boolean validateLayout()
    {
        return layoutValid.get();
    }

    protected void updateLayout()
    {
        layoutValid.set(true);

        if (parent == null)
            updateParentScreenSpace();
    }

    protected boolean validatePosition()
    {
        return posValid.get();
    }

    protected void updatePosition()
    {
        if (parent != null)
        {
            //获取用于遮罩的父级屏幕空间位置
            this.setParentScreenSpaceY(parent.getScreenSpaceY());
            this.setParentScreenSpaceX(parent.getScreenSpaceX());

            //应用父级Padding到可用空间中
            var parentPadding = parent.getPadding();

            //rectW: 可用的宽度空间
            var rectW = parent.getRenderWidth() - parentPadding.left - parentPadding.right;

            //rectH: 可用的高度空间
            var rectH = parent.getRenderHeight() - parentPadding.top - parentPadding.bottom;
            this.setParentScreenSpace(new ScreenRectangle(0, 0, (int)rectW, (int)rectH));
        }
        else
        {
            updateParentScreenSpace();
        }

        //此Drawable的最终宽高
        if (relativeSizeAxes.modX)
            renderWidth = Math.round(width * parentScreenSpace.width());
        else
            renderWidth = Math.round(width);

        if (relativeSizeAxes.modY)
            renderHeight = Math.round(height * parentScreenSpace.height());
        else
            renderHeight = Math.round(height);

        var windowInstance = Minecraft.getInstance().getWindow();

        var isEmptyRect = parentScreenSpace == ScreenRectangle.empty();
        float parentRectWidth = isEmptyRect ? windowInstance.getGuiScaledWidth() : parentScreenSpace.width();
        float parentRectHeight = isEmptyRect ? windowInstance.getGuiScaledHeight() : parentScreenSpace.height();

        var rectCentre = new UVPair(parentRectWidth / 2, parentRectHeight / 2);

        float xScreenSpaceOffset = x;
        float yScreenSpaceOffset = y;

        var parentPadding = parent == null ? new MarginPadding(0) : parent.getPadding();

        // 坐标原点：左上角
        // 居中时，通过左侧Margin减去右侧Margin来取得此Drawable的X位移
        // 对父级Padding同理
        //
        // x1 左对齐：左侧Margin + 父级左侧Padding
        // x2 横轴居中：(左侧Margin - 右侧Margin) + 父级横轴空间 - (宽度 /  2) + (父级左侧Padding - 父级右侧Padding)
        // x3 右对齐：-右侧Margin + (父级横轴空间 - 宽度) - 父级右侧Padding
        var maskX = (anchor.posMask << 4) >> 4;
        if ((maskX & PosMask.x1) == PosMask.x1)
            xScreenSpaceOffset += margin.left + parentPadding.left;
        else if ((maskX & PosMask.x2) == PosMask.x2)
            xScreenSpaceOffset += margin.getCentreOffsetX() + (rectCentre.u() - this.renderWidth / 2f) + parentPadding.getCentreOffsetX();
        else if ((maskX & PosMask.x3) == PosMask.x3)
            xScreenSpaceOffset += -margin.right + (parentRectWidth - this.renderWidth) - parentPadding.left;

        // 坐标原点：左上角
        // 居中时，通过上方Margin减去下方Margin来取得此Drawable的X位移
        // 对父级Padding同理
        //
        // y1 向上对齐：上方Margin + 父级上方Padding
        // y2 纵轴居中：(上方Margin - 下方Margin) + [父级纵轴空间 - (高度 /  2)] + (父级上方Padding - 父级下方Padding)
        // y3 向下对齐：-下方Margin + (父级纵轴空间 - 高度) - 父级下方Padding
        var maskY = (anchor.posMask >> 4) << 4;
        if ((maskY & PosMask.y1) == PosMask.y1)
            yScreenSpaceOffset += margin.top + parentPadding.top;
        else if ((maskY & PosMask.y2) == PosMask.y2)
            yScreenSpaceOffset += margin.getCentreOffsetY() + (rectCentre.v() - this.renderHeight / 2f) + parentPadding.getCentreOffsetY();
        else if ((maskY & PosMask.y3) == PosMask.y3)
            yScreenSpaceOffset += - margin.bottom + (parentRectHeight - this.renderHeight) - parentPadding.bottom;

        this.xScreenSpaceOffset = xScreenSpaceOffset;
        this.yScreenSpaceOffset = yScreenSpaceOffset;

        this.screenSpaceX = parentScreenSpaceX + xScreenSpaceOffset;
        this.screenSpaceY = parentScreenSpaceY + yScreenSpaceOffset;

        posValid.set(true);
    }

    protected float xScreenSpaceOffset;
    protected float yScreenSpaceOffset;

    //endregion Position validation

    //region W/H

    protected Axes relativeSizeAxes = Axes.None;

    public Axes getRelativeSizeAxes()
    {
        return relativeSizeAxes;
    }

    public void setRelativeSizeAxes(Axes a)
    {
        this.relativeSizeAxes = a;

        invalidateLayout();
        invalidatePosition();
    }

    /**
     * 此drawable的宽度
     */
    protected float width = 1;

    /**
     * 此drawable的高度
     */
    protected float height = 1;

    /**
     * 此drawable实际绘制和处理时的宽度
     */
    protected int renderWidth;

    /**
     * 此drawable实际绘制和处理时的高度
     */
    protected int renderHeight;

    public float getWidth()
    {
        return width;
    }

    public void setWidth(float w)
    {
        if (width == w) return;

        this.width = w;
        invalidatePosition();
        invalidateLayout();
    }

    public float getRenderWidth()
    {
        var modX = this.relativeSizeAxes.modX;
        return modX ? width * getParentScreenSpace().width() : width;
    }

    public float getHeight()
    {
        return height;
    }

    public void setHeight(float h)
    {
        if (height == h) return;

        this.height = h;
        invalidatePosition();
        invalidateLayout();
    }

    public float getRenderHeight()
    {
        var modY = this.relativeSizeAxes.modY;
        return modY ? height * getParentScreenSpace().height() : height;
    }

    public void setSize(UVPair vector2f)
    {
        this.setWidth(vector2f.u());
        this.setHeight(vector2f.v());
    }

    //endregion W/H

    //region X/Y

    private int x;
    private int y;

    /**
     * 此Drawable在屏幕空间上的X值
     */
    private float screenSpaceX;

    /**
     * 此Drawable在屏幕空间上的Y值
     */
    private float screenSpaceY;

    public float getScreenSpaceX()
    {
        return screenSpaceX;
    }

    public float getScreenSpaceY()
    {
        return screenSpaceY;
    }

    /**
     * 父级Drawable在屏幕空间上的X值，用于Masking
     */
    private float parentScreenSpaceX;

    public void setParentScreenSpaceX(float parentX)
    {
        if (this.parentScreenSpaceX == parentX) return;
        this.parentScreenSpaceX = parentX;

        invalidatePosition();
    }

    /**
     * 父级Drawable在屏幕空间上的Y值，用于Masking
     */
    private float parentScreenSpaceY;

    public void setParentScreenSpaceY(float parentY)
    {
        if (this.parentScreenSpaceY == parentY) return;
        this.parentScreenSpaceY = parentY;

        invalidatePosition();
    }

    public int getX()
    {
        return x;
    }

    public void setX(int newX)
    {
        if (x == newX) return;

        this.x = newX;
        invalidatePosition();
    }

    public int getY()
    {
        return y;
    }

    public void setY(int newY)
    {
        if (y == newY) return;

        this.y = newY;
        invalidatePosition();
    }

    //endregion X/Y

    /**
     * 此Drawable的父级在屏幕上的所有可用空间
     */
    @NotNull
    private ScreenRectangle parentScreenSpace = ScreenRectangle.empty();

    protected void updateParentScreenSpace()
    {
        var windowInstance = Minecraft.getInstance().getWindow();
        parentScreenSpace = new ScreenRectangle(0, 0, windowInstance.getGuiScaledWidth(), windowInstance.getGuiScaledHeight());
    }

    /**
     * 获取此Drawable在父级屏幕上的所有可用空间
     */
    @NotNull
    public ScreenRectangle getParentScreenSpace()
    {
        if (parentScreenSpace == ScreenRectangle.empty())
        {
            var windowInstance = Minecraft.getInstance().getWindow();
            return new ScreenRectangle(0, 0, windowInstance.getGuiScaledWidth(), windowInstance.getGuiScaledHeight());
        }

        return parentScreenSpace;
    }

    /**
     * 设置父级Drawable在其相对位置上的宽高
     */
    public void setParentScreenSpace(ScreenRectangle rect)
    {
        if (this.parentScreenSpace.equals(rect))
            return;

        this.parentScreenSpace = rect;
        invalidatePosition();
    }

    //region MarginPadding

    @NotNull
    protected MarginPadding padding = new MarginPadding();

    @NotNull
    public MarginPadding getPadding()
    {
        return padding;
    }

    public void setPadding(MarginPadding padding)
    {
        padding = padding == null ? new MarginPadding() : padding;
        this.padding = padding;
    }

    @NotNull
    private MarginPadding margin = new MarginPadding();

    @NotNull
    public MarginPadding getMargin()
    {
        return margin;
    }

    public void setMargin(@NotNull MarginPadding margin)
    {
        if (this.margin.equals(margin)) return;

        this.margin = margin;
        invalidatePosition();
    }

    //endregion MarginPadding

    private boolean masking = false;
    public boolean masking()
    {
        return masking;
    }

    public void setMasking(boolean masking)
    {
        this.masking = masking;
    }

    protected void onRender(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
    }

    // 在ParentElement中，只有isMouseOver返回为true的物件才会在hoveredElement中被选中
    // 如果永远为false则永远都不会选中此物件，这样就无法接收到鼠标点击事件
    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return hovered();
    }

    private boolean hovered;

    protected boolean hovered()
    {
        return this.hovered;
    }

    protected void onHover()
    {
    }

    protected void onHoverLost()
    {
    }

    @Override
    public final void render(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        var matrices = context.pose();
        matrices.pushMatrix();

        var hovered = this.hovered;
        this.hovered = mouseX < this.screenSpaceX + width && mouseX > this.screenSpaceX
                && mouseY < this.screenSpaceY + height && mouseY > this.screenSpaceY;

        if (hovered != this.hovered)
        {
            if (this.hovered)
                onHover();
            else
                onHoverLost();
        }

        if (this.alpha.get() == 0f)
        {
            matrices.popMatrix();
            return;
        }

        //masking = false;

        try
        {
            //this.setShaderColor(context, shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3] * this.alpha.get());

            if (!validatePosition()) updatePosition();
            if (!validateLayout()) updateLayout();

            // Render parent rect
            //context.fill(parentScreenSpace.getLeft(), parentScreenSpace.getTop(),
            //        parentScreenSpace.width(), parentScreenSpace.height(),
            //        ColorUtils.forOpacity(MaterialColors.Orange500, 0.4f).getColor());

            matrices.translate(xScreenSpaceOffset, yScreenSpaceOffset);

            // Render Self rect
            //context.fill(0, 0,
             //       renderWidth, renderHeight,
              //      ColorUtils.forOpacity(MaterialColors.Cyan500, 0.4f).getColor());

            // 嵌套遮罩有问题
            if (masking)
            {
                //context.fill(sX, sY, sX + renderWidth, sY + renderHeight, MaterialColors.Blue500.getColor());

                //context.drawText(MinecraftClient.getInstance().textRenderer,
                //        "sX: %s, sY: %s, W: %s, H: %s".formatted(sX, sY, renderWidth, renderHeight),
                //        0, 0, 0xffffffff, false);

                context.enableScissor(0, 0, renderWidth, renderHeight);
            }

            this.onRender(context, mouseX, mouseY, delta);
        }
        finally
        {
            matrices.translate(-xScreenSpaceOffset, -yScreenSpaceOffset);
            matrices.popMatrix();

            //this.setShaderColor(context, shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);

            if (masking)
                context.disableScissor();
        }
    }

    //region Transforms

    private Recorder<Integer> xRec;

    public void moveTo(Vector2i position, long duration, Easing easing)
    {
        this.moveToX(position.x(), duration, easing);
        this.moveToY(position.y(), duration, easing);
    }

    public void moveToX(int x, long duration, Easing easing)
    {
        if (xRec == null)
        {
            xRec = new Recorder<>(this.x);
            xRec.onUpdate = this::setX;
        }

        Transformer.transform(xRec, x, duration, easing);
    }

    private Recorder<Integer> yRec;

    public void moveToY(int newY, long duration, Easing easing)
    {
        if (yRec == null)
        {
            yRec = new Recorder<>(this.y);
            yRec.onUpdate = this::setY;
        }

        Transformer.transform(yRec, newY, duration, easing);
    }

    private Recorder<Float> hRec;

    public void resizeHeightTo(float newH, long duration, Easing easing)
    {
        if (hRec == null)
        {
            hRec = new Recorder<>(height);
            hRec.onUpdate = this::setHeight;
        }

        Transformer.transform(hRec, newH, duration, easing);
    }

    private Recorder<Float> wRec;

    public void resizeWidthTo(float newW, long duration, Easing easing)
    {
        if (wRec == null)
        {
            wRec = new Recorder<>(width);
            wRec.onUpdate = this::setWidth;
        }

        Transformer.transform(wRec, newW, duration, easing);
    }

    public void resizeTo(UVPair wH, long duration, Easing easing)
    {
        this.resizeHeightTo(wH.u(), duration, easing);
        this.resizeWidthTo(wH.v(), duration, easing);
    }

    protected final Recorder<Float> alpha = new Recorder<Float>(1f);

    public void setAlpha(float newVal)
    {
        this.alpha.set(newVal);
    }

    public void fadeTo(float newVal, long duration, Easing easing)
    {
        Transformer.transform(alpha, MathUtils.clamp(0f, 1f, newVal), duration, easing);
    }

    public void fadeIn(long duration, Easing easing)
    {
        this.fadeTo(1, duration, easing);
    }

    public void fadeOut(long duration, Easing easing)
    {
        this.fadeTo(0, duration, easing);
    }

    //endregion Transforms

    public void dispose()
    {
    }

    //region Element

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation)
    {
        return this.isFocused()
               ? null
               : ComponentPath.leaf(this);
    }

    @Override
    public ScreenRectangle getRectangle()
    {
        return new ScreenRectangle(this.getX(), this.getY(), Math.round(this.getRenderWidth()), Math.round(this.getHeight()));
    }

    private boolean focused;

    @Override
    public void setFocused(boolean focused)
    {
        this.focused = focused;
    }

    @Override
    public boolean isFocused()
    {
        return focused;
    }

    //endregion Element

    //region Selectable

    @Override
    public NarrationPriority narrationPriority()
    {
        return focused
               ? NarrationPriority.FOCUSED
               : hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder)
    {
    }

    //endregion Selectable
}

