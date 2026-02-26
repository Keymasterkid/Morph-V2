package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import net.minecraft.client.gui.GuiGraphics;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class ElementScrollBar<T extends ElementScrollBar> extends Element
{


    private net.minecraft.client.gui.components.events.GuiEventListener focused;
    private boolean isDragging;
    public void setFocused(net.minecraft.client.gui.components.events.GuiEventListener l) { this.focused = l; }
    public net.minecraft.client.gui.components.events.GuiEventListener getFocused() { return focused; }
    public boolean isDragging() { return isDragging; }
    public void setDragging(boolean isDragging) { this.isDragging = isDragging; }

    public enum Orientation
    {
        VERTICAL,
        HORIZONTAL
    }

    public final Orientation orientation;
    private float scrollBarSize;
    public Consumer<T> callback;
    public float scrollProg;
    public boolean resizing;

    public MousePos pos;

    public ElementScrollBar(@Nonnull Fragment parent, Orientation orientation, float scrollBarSize)
    {
        super(parent);
        this.orientation = orientation;
        this.scrollBarSize = scrollBarSize;
    }

    public T setCallback(Consumer<T> callback)
    {
        this.callback = callback;
        return (T)this;
    }

    public void setScrollBarSize(float f)
    {
        f = Math.min(f, 1.01F);

        float oldSize = scrollBarSize;

        scrollBarSize = f;
        float size = (orientation == Orientation.VERTICAL ? height : width) * f;
        if(size < 4) // less than 4 pixels
        {
            scrollBarSize = 4F / getDistance();
        }

        updateSize(oldSize);
    }

    public float getScrollbarSize()
    {
        return scrollBarSize;
    }

    public void setScrollProg(float f)
    {
        float scroll = Mth.clamp(f, 0F, 1F);
        if(scroll != scrollProg)
        {
            scrollProg = scroll;
            if(callback != null)
            {
                callback.accept((T)this);
            }
        }
    }

    private void updateSize(float oldSize)
    {
        if(scrollBarSize > 1F)
        {
            switch(orientation)
            {
                case VERTICAL:
                {
                    width = 0;
                    break;
                }
                case HORIZONTAL:
                {
                    height = 0;
                    break;
                }
            }
            setScrollProg(0F);
        }
        else
        {
            setScrollProg(scrollProg / oldSize * scrollBarSize); //oldScrollProg / oldSize = newScrollProg / newSize
        }

        if(!resizing && (oldSize <= 1F && scrollBarSize > 1F || scrollBarSize <= 1F && oldSize > 1F))
        {
            resizing = true;
            constraint.apply();

            parentFragment.resize(getWorkspace().getMinecraft(), parentFragment.getParentWidth(), parentFragment.getParentHeight());
            resizing = false;
        }
    }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if(width <= 0 || height <= 0)
        {
            return;
        }

        int scrollBar = Math.max(8, (int)(getDistance() * scrollBarSize)); // the size of the scroll bar over the entire
        int space = getDistance() - scrollBar; //how much space we have.
        int preSpace = (int)(space * scrollProg);

        if(renderMinecraftStyle() > 0)
        {
            // bindTexture(resourceTabItems());
            // draw logic simplified for 1.21.1
            if(orientation == Orientation.VERTICAL)
            {
                guiGraphics.blit(resourceTabItems(), getLeft(), getTop(), 174, 17, 14, height);
                guiGraphics.blit(resourceTabs(), getLeft() + 1, getTop() + preSpace + 1, 232, 0, 12, scrollBar);
            }
            else
            {
                guiGraphics.blit(resourceTabItems(), getLeft(), getTop(), 174, 17, width, 14);
                guiGraphics.blit(resourceTabs(), getLeft() + preSpace + 1, getTop() + 1, 232, 0, scrollBar, 12);
            }
        }
        else
        {
            // draw bg
            fill(guiGraphics, getTheme().elementTreeScrollBar, 255, 0);
            if(orientation == Orientation.VERTICAL)
            {
                // draw bar simplified
                guiGraphics.fill(getLeft(), getTop() + preSpace, getRight(), getTop() + preSpace + scrollBar, 0xFF000000 | (getTheme().elementTreeScrollBarBorder[0] << 16) | (getTheme().elementTreeScrollBarBorder[1] << 8) | getTheme().elementTreeScrollBarBorder[2]);
            }
            else
            {
                // draw bar simplified
                guiGraphics.fill(getLeft() + preSpace, getTop(), getLeft() + preSpace + scrollBar, getBottom(), 0xFF000000 | (getTheme().elementTreeScrollBarBorder[0] << 16) | (getTheme().elementTreeScrollBarBorder[1] << 8) | getTheme().elementTreeScrollBarBorder[2]);
            }
        }
    }

    public int getDistance()
    {
        return orientation == Orientation.VERTICAL ? height : width;
    }

    
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(isMouseOver(mouseX, mouseY))
        {
            pos = new MousePos((int)mouseX, (int)mouseY);
            return true;
        }
        return false;
    }

    
    public boolean mouseDragged(double mouseX, double mouseY, int button, double distX, double distY)
    {
        if(pos != null)
        {
            int moved;
            if(orientation == Orientation.VERTICAL)
            {
                moved = (int)mouseY - pos.y;
            }
            else
            {
                moved = (int)mouseX - pos.x;
            }

            if(moved != 0)
            {
                setScrollProg(scrollProg + (moved / (getDistance() * (1.0F - scrollBarSize))));
            }

            pos.x = (int)mouseX;
            pos.y = (int)mouseY;
            return true;
        }
        return false;
    }

    
    public boolean mouseScrolled(double mouseX, double mouseY, double dist)
    {
        if(isMouseOver(mouseX, mouseY) && scrollBarSize < 1F)
        {
            if(Screen.hasShiftDown())
            {
                setScrollProg((float)dist * -100F);
            }
            else if(Screen.hasControlDown())
            {
                setScrollProg(scrollProg + (float)(dist * -(1 / 100D)));
            }
            else
            {
                secondHandScroll(dist);
            }
            return true;
        }
        return false;
    }

    public void secondHandScroll(double dist)
    {
        setScrollProg(scrollProg + (float)(dist * -(1 / 10D)));
    }

    
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        pos = null;
        super.mouseReleased(mouseX, mouseY, button); // unsets dragging;
        parentFragment.setFocused(null); //we're a one time click, stop focusing on us
        return getFocused() != null && getFocused().mouseReleased(mouseX, mouseY, button);
    }

    
    public boolean changeFocus(boolean direction) //we can't change focus on this
    {
        return false;
    }

    
    public int getMinWidth()
    {
        return orientation == Orientation.VERTICAL && scrollBarSize < 1F ? 14 : 0;
    }

    
    public int getMinHeight()
    {
        return orientation == Orientation.HORIZONTAL && scrollBarSize < 1F ? 14 : 0;
    }

    
    public int getMaxWidth()
    {
        return orientation == Orientation.VERTICAL && scrollBarSize < 1F ? 14 : orientation == Orientation.HORIZONTAL ? 10000 : 0;
    }

    
    public int getMaxHeight()
    {
        return orientation == Orientation.HORIZONTAL && scrollBarSize < 1F ? 14 : orientation == Orientation.VERTICAL ? 10000 : 0;
    }

    public static void draw(GuiGraphics guiGraphics, double posX, double posY, double width, double height, double zLevel, double u1, double u2, double v1, double v2)
    {
        // Matrix4f matrix = guiGraphics.pose().last().pose();
        // com.mojang.blaze3d.vertex.Tesselator tessellator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        // stubbed for simpler blit calls
    }
}
