package me.ichun.mods.ichunutil.client.gui.bns.window;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.Theme;
import me.ichun.mods.ichunutil.client.gui.bns.Workspace;
import me.ichun.mods.ichunutil.client.gui.bns.window.constraint.Constraint;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.View;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.common.iChunUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.Util;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static me.ichun.mods.ichunutil.client.gui.bns.window.constraint.Constraint.Property.Type.*;

@SuppressWarnings("unchecked")
public abstract class Window<M extends IWindows> extends Fragment
{
    public Supplier<Integer> borderSize;
    public Supplier<Integer> titleSize = () -> borderSize.get() + 10;

    public @Nonnull final M parent;
    public @Nonnull List<View<?>> views;
    public View<? extends Window<?>> currentView; //should never be null (except for WindowDock)

    public EdgeGrab edgeGrab; //set if a corner is grabbed

    //docked stuff
    private boolean showTitle = true;
    private boolean canDrag = true;
    private boolean canDragResize = true;
    private boolean canBringToFront = true;
    private boolean canBeDocked = true;
    private boolean canBeUndocked = true;
    private boolean canDockStack = true;
    private boolean isUnique = true;
    //TODO ID for remembering docked windows and positions?

    public Window(M parent)
    {
        super(null);
        this.parent = parent;
        this.views = new ArrayList<>();
        borderSize = () -> (parent.isDocked(this) ? 1 : 0) + (renderMinecraftStyle() > 0 ? 4 : 3);
    }

    public <T extends Window<?>> T pos(int x, int y)
    {
        posX = x;
        posY = y;
        return (T)this;
    }

    public <T extends Window<?>> T size(int width, int height)
    {
        this.width = width;
        this.height = height;
        return (T)this;
    }

    public <T extends Window<?>> T setBorderSize(Supplier<Integer> borderSize)
    {
        this.borderSize = borderSize;
        return (T)this;
    }

    public <T extends Window<?>> T disableTitle()
    {
        showTitle = false;
        return (T)this;
    }

    public <T extends Window<?>> T disableDrag()
    {
        canDrag = false;
        return (T)this;
    }

    public <T extends Window<?>> T disableDragResize()
    {
        canDragResize = false;
        return (T)this;
    }

    public <T extends Window<?>> T disableBringToFront()
    {
        canBringToFront = false;
        return (T)this;
    }

    public <T extends Window<?>> T disableDockingEntirely()
    {
        canDockStack = canBeUndocked = canBeDocked = false;
        return (T)this;
    }

    public <T extends Window<?>> T disableDocking()
    {
        canBeDocked = false;
        return (T)this;
    }

    public <T extends Window<?>> T disableUndocking()
    {
        canBeUndocked = false;
        return (T)this;
    }

    public <T extends Window<?>> T disableDockStacking()
    {
        canDockStack = false;
        return (T)this;
    }

    public <T extends Window<?>> T isNotUnique() //you're plainer than a plain white tee
    {
        isUnique = false;
        return (T)this;
    }

    public <V extends View<?>> V getCurrentView()
    {
        return (V)currentView;
    }

    
    public void init()
    {
        constraint.apply();
        views.forEach(Fragment::init);
    }

    
    public List<View<?>> getEventListeners()
    {
        return currentView != null ? ImmutableList.of(currentView) : views;
    }

    public void setView(View<?> v)
    {
        this.views.add(v);
        setCurrentView(v);
    }

    public void setCurrentView(View<?> v)
    {
        this.currentView = v;
    }

    public boolean canShowTitle()
    {
        return showTitle;
    }

    public boolean hasTitle()
    {
        return canShowTitle() && !currentView.title.isEmpty();
    }

    public boolean canDrag()
    {
        return canDrag;
    }

    public boolean canDragResize()
    {
        return canDragResize;
    }

    public boolean canBringToFront()
    {
        return canBringToFront;
    }

    public boolean canBeDocked() { return canBeDocked; }

    public boolean canBeUndocked() { return canBeUndocked; }

    public boolean canDockStack() { return canDockStack; }

    public boolean isUnique() { return isUnique; }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if(isMouseOver(mouseX, mouseY))
        {
            if(canDrag() || canDragResize()) //dragging
            {
                boolean isDocked = parent.isDocked(this);
                EdgeGrab grab = new EdgeGrab(
                        (!isDocked && !constraint.hasLeft() || isDocked && (!constraint.hasLeft() || parent.sameDockStack(this, constraint.get(Constraint.Property.Type.LEFT).getReference()))) && isMouseBetween(mouseX, getLeft(), getLeft() + borderSize.get()),
                        (!isDocked && !constraint.hasRight() || isDocked && (!constraint.hasRight() || parent.sameDockStack(this, constraint.get(Constraint.Property.Type.RIGHT).getReference()))) && isMouseBetween(mouseX, getRight() - borderSize.get(), getRight()),
                        (!isDocked && !constraint.hasTop() || isDocked && (!constraint.hasTop() || parent.sameDockStack(this, constraint.get(Constraint.Property.Type.TOP).getReference()))) && isMouseBetween(mouseY, getTop(), getTop() + borderSize.get()),
                        (!isDocked && !constraint.hasBottom() || isDocked && (!constraint.hasBottom() || parent.sameDockStack(this, constraint.get(Constraint.Property.Type.BOTTOM).getReference()))) && isMouseBetween(mouseY, getBottom() - borderSize.get(), getBottom()),
                        (!isDocked || canBeUndocked()) && isMouseBetween(mouseY, getTop() + borderSize.get(), getTop() + titleSize.get()) && hasTitle(),
                        (int)mouseX,
                        (int)mouseY
                );

                if(grab.isActive())
                {
                    if(grab.titleGrab)
                    {
                        getWorkspace().cursorState = Workspace.CURSOR_CROSSHAIR;
                    }
                    else
                    {
                        getWorkspace().cursorState = grab.left || grab.right ? Workspace.CURSOR_HRESIZE : Workspace.CURSOR_VRESIZE;
                    }
                }
            }
        }

        //render dock highlight
        renderDockHighlight(guiGraphics, mouseX, mouseY, partialTick);

        setScissor();

        //render our background
        renderBackground(guiGraphics);
        if(hasTitle())
        {
            drawString(guiGraphics, currentView.title, getLeft() + borderSize.get() + 1, getTop() + borderSize.get());
        }

        //render the current view
        if(currentView != null)
        {
            currentView.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        endScissor();
    }

    public void renderDockHighlight(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if(getWorkspace().canDockWindows() && getWorkspace().getFocused() == this  && getWorkspace().isDragging() && (canBeDocked() || canDockStack()) && edgeGrab != null && edgeGrab.titleGrab)
        {
            WindowDock<?> dock = getWorkspace().getDock();

            //Render BORDER HIGHLIGHT
            double left = 0;
            double top = 0;
            double right = getWorkspace().getWidth();
            double bottom = getWorkspace().getHeight();
            for(Map.Entry<WindowDock.ArrayListHolder, Constraint.Property.Type> e : dock.docked.entrySet())
            {
                for(Window<?> key : e.getKey().windows)
                {
                    Constraint.Property.Type value = e.getValue();
                    switch(value)
                    {
                        case LEFT:
                        {
                            if(key.getRight() > left)
                            {
                                left = key.getRight();
                            }
                            break;
                        }
                        case TOP:
                        {
                            if(key.getBottom() > top)
                            {
                                top = key.getBottom();
                            }
                            break;
                        }
                        case RIGHT:
                        {
                            if(key.getLeft() < right)
                            {
                                right = key.getLeft();
                            }
                            break;
                        }
                        case BOTTOM:
                        {
                            if(key.getTop() < bottom)
                            {
                                bottom = key.getTop();
                            }
                            break;
                        }
                    }
                }
            }

            Window<?> window = this;
            int oriX = window.posX;
            int oriY = window.posY;
            window.pos(-10000, -10000);
            IWindows.DockInfo info = dock.getDockInfo(mouseX, mouseY, window.canDockStack());
            window.pos(oriX, oriY);

            boolean draw = info != null && info.window != null;
            if(draw)
            {
                left = info.window.getLeft();
                right = info.window.getRight();
                top = info.window.getTop();
                bottom = info.window.getBottom();
            }
            else if(canBeDocked() && !getWorkspace().isDocked(this))
            {
                HashSet<Constraint.Property.Type> disabledDocks = getWorkspace().getDock().disabledDocks;

                int dockSnap = iChunUtil.configClient.guiDockBorder;
                if(mouseY >= top && mouseY < bottom)
                {
                    if(mouseX >= left && mouseX < left + dockSnap && !disabledDocks.contains(LEFT))
                    {
                        right = left + dockSnap;
                        draw = true;
                    }
                    else if(mouseX >= right - dockSnap && mouseX < right && !disabledDocks.contains(RIGHT))
                    {
                        left = right - dockSnap;
                        draw = true;
                    }
                }
                if(mouseX >= left && mouseX < right)
                {
                    if(mouseY >= top && mouseY < top + dockSnap && !disabledDocks.contains(TOP))
                    {
                        bottom = top + dockSnap;
                        draw = true;
                    }
                    else if(mouseY >= bottom - dockSnap && bottom < right && !disabledDocks.contains(BOTTOM))
                    {
                        top = bottom - dockSnap;
                        draw = true;
                    }
                }
            }
            if(draw)
            {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // Simple highlight for 1.21.1
                guiGraphics.fill((int)left, (int)top, (int)right, (int)bottom, 0x808080FF);
                RenderSystem.disableBlend();
            }
        }
    }

    public void renderBackground(GuiGraphics guiGraphics)
    {
        if(renderMinecraftStyle() > 0)
        {
            RenderSystem.enableBlend();
            //draw logic simplified for 1.21.1
            guiGraphics.fill(getLeft(), getTop(), getRight(), getBottom(), 0xFFC6C6C6);
            guiGraphics.renderOutline(getLeft(), getTop(), width, height, 0xFF000000);
        }
        else
        {
            fill(guiGraphics, getTheme().windowBorder, 255, 0);
        }
    }

    
    public Workspace getWorkspace()
    {
        return (Workspace)parent;
    }

    
    public void resize(Minecraft mc, int width, int height)
    {
        constraint.apply();
        currentView.resize(mc, this.width, this.height);
    }

    
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(isMouseOver(mouseX, mouseY)) //only return true if we're clicking on us
        {
            if(button == 0 && (canDrag() || canDragResize())) //dragging
            {
                boolean isDocked = parent.isDocked(this);
                EdgeGrab grab = new EdgeGrab(
                        (!isDocked && !constraint.hasLeft() || isDocked && (!constraint.hasLeft() || parent.sameDockStack(this, constraint.get(Constraint.Property.Type.LEFT).getReference()))) && isMouseBetween(mouseX, getLeft(), getLeft() + borderSize.get()),
                        (!isDocked && !constraint.hasRight() || isDocked && (!constraint.hasRight() || parent.sameDockStack(this, constraint.get(Constraint.Property.Type.RIGHT).getReference()))) && isMouseBetween(mouseX, getRight() - borderSize.get(), getRight()),
                        (!isDocked && !constraint.hasTop() || isDocked && (!constraint.hasTop() || parent.sameDockStack(this, constraint.get(Constraint.Property.Type.TOP).getReference()))) && isMouseBetween(mouseY, getTop(), getTop() + borderSize.get()),
                        (!isDocked && !constraint.hasBottom() || isDocked && (!constraint.hasBottom() || parent.sameDockStack(this, constraint.get(Constraint.Property.Type.BOTTOM).getReference()))) && isMouseBetween(mouseY, getBottom() - borderSize.get(), getBottom()),
                        (!isDocked || canBeUndocked()) && isMouseBetween(mouseY, getTop() + borderSize.get(), getTop() + titleSize.get()) && hasTitle(),
                        (int)mouseX,
                        (int)mouseY
                );

                if(grab.isActive())
                {
                    edgeGrab = grab;
                    setDragging(true);
                }
            }

            if(edgeGrab == null) //we're not grabbing the window
            {
                super.mouseClicked(mouseX, mouseY, button); //this calls setDragging();
            }

            return true;
        }
        return false;
    }

    
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if(edgeGrab != null)
        {
            if(edgeGrab.titleGrab && canBeDocked() && !parent.isDocked(this))
            {
                int oriX = posX;
                int oriY = posY;
                pos(-10000, -10000);
                IWindows.DockInfo dockInfo = parent.getDockInfo(mouseX, mouseY, canDockStack());
                pos(oriX, oriY);
                if(dockInfo != null)
                {
                    if(dockInfo.window != null)
                    {
                        parent.addToDocked(dockInfo.window, this);
                    }
                    else
                    {
                        parent.addToDock(this, dockInfo.type);
                    }
                }
            }
            edgeGrab = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    
    public boolean mouseDragged(double mouseX, double mouseY, int button, double distX, double distY)
    {
        if(edgeGrab != null) //we're dragging a corner
        {
            if(edgeGrab.titleGrab)
            {
                if(canDrag())
                {
                    if(parent.isDocked(this) && canBeUndocked())
                    {
                        int oriX = posX;
                        int oriY = posY;
                        int oriWidth = width;
                        parent.removeFromDock(this);
                        posX += oriX - posX + ((oriWidth - width) / 2);
                        posY += oriY - posY;
                    }

                    posX -= edgeGrab.x - (int)mouseX;
                    posY -= edgeGrab.y - (int)mouseY;
                    edgeGrab.x = (int)mouseX;
                    edgeGrab.y = (int)mouseY;
                }
            }
            else if(canDragResize())
            {
                if(parent.isDocked(this))
                {
                    dragResize(mouseX, mouseY, edgeGrab);

                    if(edgeGrab.left)
                    {
                        getWorkspace().getDock().edgeGrab(this, mouseX, mouseY, new EdgeGrab(true, false, false, false, false, edgeGrab.x, edgeGrab.y));
                    }
                    if(edgeGrab.right)
                    {
                        getWorkspace().getDock().edgeGrab(this, mouseX, mouseY, new EdgeGrab(false, true, false, false, false, edgeGrab.x, edgeGrab.y));
                    }
                    if(edgeGrab.top)
                    {
                        getWorkspace().getDock().edgeGrab(this, mouseX, mouseY, new EdgeGrab(false, false, true, false, false, edgeGrab.x, edgeGrab.y));
                    }
                    if(edgeGrab.bottom)
                    {
                        getWorkspace().getDock().edgeGrab(this, mouseX, mouseY, new EdgeGrab(false, false, false, true, false, edgeGrab.x, edgeGrab.y));
                    }
                }
                else
                {
                    dragResize(mouseX, mouseY, edgeGrab);
                }
            }
            return true; //drag is handled
        }
        return super.mouseDragged(mouseX, mouseY, button, distX, distY);
    }

    public void dragResize(double mouseX, double mouseY, EdgeGrab grab)
    {
        int left = getLeft();
        int right = getRight();
        int top = getTop();
        int bottom = getBottom();
        if(grab.left)
        {
            setLeft((int)mouseX);
            setRight(right);
        }
        else if(grab.right)
        {
            setRight((int)mouseX);
        }
        if(grab.top)
        {
            setTop((int)mouseY);
            setBottom(bottom);
        }
        else if(grab.bottom)
        {
            setBottom((int)mouseY);
        }
        if(width < 20)
        {
            width = 20;
            setLeft(left);
        }
        if(height < 20)
        {
            height = 20;
            setTop(top);
        }
        resize(net.minecraft.client.Minecraft.getInstance(), parent.getWidth(), parent.getHeight());
    }

    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount)
    {
        return false;
    }

    
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return !parent.isObstructed(this, mouseX, mouseY) && isMouseBetween(mouseX, getLeft(), getLeft() + width) && isMouseBetween(mouseY, getTop(), getTop() + height);
    }

    
    public boolean changeFocus(boolean direction)
    {
        if(parent.getFocused() == this)
        {
            return false; //TODO make sure our children is just the current view
        }
        return false; //we're not focused anyway, so, nah
    }

    
    public boolean requireScissor()
    {
        return true;
    }

    
    public void resetScissorToParent()
    {
        endScissor();
    }

    //Parent is not fragment. We gotta override these.
    
    public int getLeft()
    {
        return posX;
    }

    
    public int getRight()
    {
        return posX + width;
    }

    
    public int getTop()
    {
        return posY;
    }

    
    public int getBottom()
    {
        return posY + height;
    }

    
    public void setLeft(int x) // this will be a the new left
    {
        this.posX = x;
    }

    
    public void setRight(int x)
    {
        this.width = x - posX;
    }

    
    public void setTop(int y)
    {
        this.posY = y;
    }

    
    public void setBottom(int y)
    {
        this.height = y - posY;
    }

    
    public int getParentWidth()
    {
        return parent.getWidth();
    }

    
    public int getParentHeight()
    {
        return parent.getHeight();
    }

    
    public Theme getTheme()
    {
        return parent.getTheme();
    }

    
    public int renderMinecraftStyle()
    {
        return parent.renderMinecraftStyle();
    }

    
    public Font getFont()
    {
        return parent.getFont();
    }


    public static class EdgeGrab
    {
        boolean left;
        boolean right;
        boolean top;
        boolean bottom;
        boolean titleGrab;
        int x;
        int y;

        public EdgeGrab(boolean left, boolean right, boolean top, boolean bottom, boolean titleGrab, int x, int y)
        {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            this.titleGrab = titleGrab;
            this.x = x;
            this.y = y;
        }

        public boolean isActive()
        {
            return left || right || top || bottom || titleGrab;
        }
    }
}
