package me.ichun.mods.ichunutil.client.gui.bns;

import com.google.common.base.Splitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.window.*;
import me.ichun.mods.ichunutil.client.gui.bns.window.constraint.Constraint;
import me.ichun.mods.ichunutil.client.gui.bns.window.constraint.IConstrainable;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.common.iChunUtil;
import me.ichun.mods.ichunutil.common.util.IOUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import org.joml.Matrix4f;
import net.minecraft.network.chat.*;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class Workspace extends Screen //boxes and stuff!
        implements IConstrainable, IWindows
{
    public static final long CURSOR_ARROW = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
    public static final long CURSOR_IBEAM = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
    public static final long CURSOR_CROSSHAIR = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
    public static final long CURSOR_HAND = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
    public static final long CURSOR_HRESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
    public static final long CURSOR_VRESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);

    public static final String ELLIPSIS = "\u2026";//"…";

    private static HashMap<Class<?>, Function<Object, List<String>>> OBJECT_INTERPRETER = Util.make(new HashMap<>(), m -> {
        m.put(File.class, (o) -> {
            File file = (File)o;
            List<String> info = new ArrayList<>();
            info.add(file.getName());
            info.add((new SimpleDateFormat()).format(new Date(file.lastModified())));
            info.add(IOUtil.readableFileSize(file.length()));
            return info;
        });
        m.put(Theme.class, (o) -> Collections.singletonList(((Theme)o).name + " - " + ((Theme)o).author));
        m.put(Entity.class, (o) -> Collections.singletonList(((Entity)o).getDisplayName().getString()));
        m.put(Class.class, (o) -> Collections.singletonList(((Class)o).getSimpleName()));
    });

    public static @Nonnull List<String> getInterpretedInfo(Object o)
    {
        Map.Entry<Class<?>, Function<Object, List<String>>> lastEntryUsed = null;
        List<String> infos = null;
        for(Map.Entry<Class<?>, Function<Object, List<String>>> e : OBJECT_INTERPRETER.entrySet())
        {
            if(e.getKey().isInstance(o))
            {
                if(!(lastEntryUsed != null && e.getKey().isAssignableFrom(lastEntryUsed.getKey()))) // !(the last entry extends our current class)
                {
                    lastEntryUsed = e;
                    infos = e.getValue().apply(o);
                }
            }
        }
        if(infos == null)
        {
            infos = new ArrayList<>();
            infos.add(o.toString());
        }
        return infos;
    }

    public static void registerObjectInterpreter(Class<?> clz, Function<Object, List<String>> function) //TODO register Entities for .getName()
    {
        OBJECT_INTERPRETER.put(clz, function);
    }

    public int ellipsisLength = 0;

    private Theme theme = Theme.getInstance();
    public ArrayList<Window<?>> windows = new ArrayList<>(); //0 = newest
    protected int renderMinecraftStyle;
    private boolean hasInit;

    private Screen lastScreen;

    public String lastTooltip;
    public int tooltipCooldown;

    public long cursorState;

    public Workspace(Screen lastScreen, Component title, int mcStyle)
    {
        super(title);
        this.lastScreen = lastScreen;
        renderMinecraftStyle = mcStyle;

        if(canDockWindows())
        {
            windows.add(new WindowDock<>(this));
        }
    }

    public <T extends Workspace> T setLastScreen(Screen screen)
    {
        this.lastScreen = screen;
        return (T)this;
    }

    public <T extends Workspace> T setTheme(Theme theme)
    {
        this.theme = theme;
        return (T)this;
    }

    public <T extends Workspace> T setMinecraftStyle(int i)
    {
        this.renderMinecraftStyle = i;
        return (T)this;
    }

    
    public int getWidth()
    {
        return width;
    }

    
    public int getHeight()
    {
        return height;
    }

    
    public Theme getTheme()
    {
        return theme;
    }

    
    public void closeScreen()
    {
        this.minecraft.setScreen(lastScreen);
    }

    
    protected void init()
    {
        if(!hasInit)
        {
            hasInit = true;
            ellipsisLength = net.minecraft.client.Minecraft.getInstance().font.width(ELLIPSIS);

            for(me.ichun.mods.ichunutil.client.gui.bns.window.Window<?> w : windows) w.init();
        }
        // // keyboard events removed
    }

    public boolean hasInit()
    {
        return hasInit;
    }

    
    public void onClose()
    {
        // // keyboard events removed

        GLFW.glfwSetCursor(this.minecraft.getWindow().getWindow(), 0);
    }

    
    public List<Window<?>> getEventListeners()
    {
        if(canDockWindows())
        {
            ArrayList<Window<?>> winds = new ArrayList<>();
            for(int i = 0; i < windows.size(); i++)
            {
                Window<?> window = windows.get(i);
                if(window instanceof WindowDock)
                {
                    ((WindowDock<?>)window).docked.keySet().forEach(h -> winds.addAll(h.windows));
                }
                else
                {
                    winds.add(window);
                }
            }
            winds.remove(getDock());
            return winds;
        }
        return windows;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return getEventListeners();
    }

    
    public Window<?> addWindow(Window<?> window)
    {
        if(window.isUnique()) // aw how cute
        {
            List<Window<?>> allWindows = getEventListeners();
            for(int i = allWindows.size() - 1; i >= 0; i--)
            {
                Window<?> window1 = allWindows.get(i);
                if(window1.getClass() == window.getClass()) //we're unique. Kill the old one
                {
                    if(isDocked(window1))
                    {
                        window1.onClose();
                        getDock().removeFromDock(window1); //Don't call our own removeFromDock, that readds it back into our list.
                    }
                    else
                    {
                        removeWindow(window1);
                    }
                }
            }
        }
        windows.add(0, window); //MC's iterator starts from first element of list
        return window;
    }

    
    public void removeWindow(Window<?> window)
    {
        if(getFocused() == window)
        {
            setFocused(null);
        }
        window.onClose(); //TODO this might bite me in the ass. how can we tell if the window was removed or destroyed????? dock???
        windows.remove(window);
    }

    public void bringToFront(Window<?> window)
    {
        if(window.canBringToFront() && windows.remove(window))
        {
            addWindow(window);
        }
    }

    public void putInCenter(Window<?> window)
    {
        if(!isDocked(window))
        {
            window.pos((int)((getWidth() - window.getWidth()) / 2D), (int)((getHeight() - window.getHeight()) / 2D));
        }
    }

    public void openWindowInCenter(Window<?> window, double widthRatio, double heightRatio, boolean greyout)
    {
        if(widthRatio <= 1D)
        {
            window.setWidth((int)(window.getParentWidth() * widthRatio));
        }
        else
        {
            window.setWidth((int)widthRatio);
        }
        if(heightRatio <= 1D)
        {
            window.setHeight((int)(window.getParentHeight() * heightRatio));
        }
        else
        {
            window.setHeight((int)heightRatio);
        }

        if(greyout)
        {
            addWindowWithGreyout(window);
        }
        else
        {
            addWindow(window);
        }
        putInCenter(window);
        setFocused(window);

        window.init();
    }

    public void openWindowInCenter(Window<?> window, double widthRatio, double heightRatio)
    {
        openWindowInCenter(window, widthRatio, heightRatio, false);
    }

    public void openWindowInCenter(Window<?> window, boolean greyout)
    {
        openWindowInCenter(window, 0.5D, 0.5D, greyout);
    }

    public void openWindowInCenter(Window<?> window)
    {
        openWindowInCenter(window, false);
    }

    public void addWindowWithGreyout(Window<?> window)
    {
        WindowGreyout<?> greyout = new WindowGreyout<>(this, window);
        addWindow(greyout);
        greyout.init();

        addWindow(window);
    }

    
    public void tick()
    {
        for(Object f : getEventListeners()) ((me.ichun.mods.ichunutil.client.gui.bns.window.Fragment<?>)f).tick();
        tooltipCooldown--;
    }

    public @Nullable <T extends Fragment<?>> T getById(@Nonnull String id)
    {
        Fragment<?> o = null;
        for(GuiEventListener child : children())
            {
                if(child instanceof Fragment)
                {
                    o = ((Fragment<?>)child).getById(id);
                }
            }
        return (T)o;
    }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        cursorState = CURSOR_ARROW;

        guiGraphics.pose().pushPose();
        RenderSystem.enableBlend();
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        renderWindows(guiGraphics, mouseX, mouseY, partialTick);

        renderTooltip(guiGraphics, mouseX, mouseY, partialTick);

        resetBackground();
        RenderSystem.enableBlend();
        guiGraphics.pose().popPose();

        GLFW.glfwSetCursor(this.minecraft.getWindow().getWindow(), cursorState);
    }

    public void renderWindows(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        for(int i = windows.size() - 1; i >= 0; i--)
        {
            Window<?> window = windows.get(i);
            guiGraphics.pose().translate(0D, 0D, 10D);
            window.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        //render tooltip
        me.ichun.mods.ichunutil.client.gui.bns.window.Fragment<?> topMost = getTopMostFragment(mouseX, mouseY);
        if(topMost != null)
        {
            String tooltip = topMost.tooltip(mouseX, mouseY);
            if(tooltip != null)
            {
                if(!tooltip.equals(lastTooltip))
                {
                    lastTooltip = tooltip;
                    tooltipCooldown = iChunUtil.configClient.guiTooltipCooldown;
                }
            }
            else
            {
                lastTooltip = null;
            }
        }

        if(lastTooltip != null && tooltipCooldown < 0)
        {
            renderTooltip(guiGraphics, lastTooltip, mouseX, mouseY);
        }
    }

    public void renderTooltip(GuiGraphics guiGraphics, @Nonnull String tooltip, int mouseX, int mouseY)
    {
        List<String> textStrings = Splitter.on("\n").splitToList(tooltip);
        // guiGraphics.renderTooltip stubbed/simplified
        if(renderMinecraftStyle > 0)
        {
            List<Component> textLines = new ArrayList<>();
            for(String s : textStrings)
            {
                textLines.add(Component.literal(s));
            }
            // guiGraphics.renderTooltip(font, textLines, Optional.empty(), mouseX, mouseY);
        }
        else //Mostly taken from GuiUtils
        {
            // Stubbed custom tooltip rendering
        }
    }

    public @Nullable me.ichun.mods.ichunutil.client.gui.bns.window.Fragment<?> getTopMostFragment(double mouseX, double mouseY)
    {
        me.ichun.mods.ichunutil.client.gui.bns.window.Fragment<?> o = null;
        List<Window<?>> children = getEventListeners();
        for(int i = children.size() - 1; i >= 0; i--) //furthest back to front
        {
            me.ichun.mods.ichunutil.client.gui.bns.window.Fragment<?> o1 = null;
            if(o1 != null)
            {
                o = o1;
            }
        }
        return o;
    }

    
    public void resize(Minecraft mc, int width, int height)
    {
        this.minecraft = mc;
        // mc.getItemRenderer() removed
        this.font = mc.font;
        this.width = width;
        this.height = height;
        this.setFocused(null);

        //resize windows
        windows.forEach(window -> window.resize(mc, width, height));
    }

    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if(renderMinecraftStyle > 0)
        {
            // super.renderBackground removed
            // Simple background render
            guiGraphics.fill(0, 0, width, height, 0x80000000);
        }
        else
        {
            RenderSystem.clearColor((float)getTheme().workspaceBackground[0] / 255F, (float)getTheme().workspaceBackground[1] / 255F, (float)getTheme().workspaceBackground[2] / 255F, 255F);
        }
    }

    public void resetBackground()
    {
        /* popMatrix removed */

        if(renderMinecraftStyle == 0)
        {
            // RenderSystem.matrixMode(GL11.GL_PROJECTION);
            // RenderSystem.loadIdentity();
            // RenderSystem.ortho(0.0D, minecraft.getWindow().getWidth() / minecraft.getWindow().getGuiScaleFactor(), minecraft.getWindow().getHeight() / minecraft.getWindow().getGuiScaleFactor(), 0.0D, 1000.0D, 3000.0D);
            // RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            // RenderSystem.loadIdentity();
            /* translatef removed */
        }
    }

    
    public Font getFont()
    {
        return font;
    }

    
    public int renderMinecraftStyle()
    {
        return renderMinecraftStyle;
    }

    //TODO do we want to pass in escape??


    
    public boolean mouseDragged(double mouseX, double mouseY, int button, double distX, double distY)
    {
        return super.mouseDragged(mouseX, mouseY, button, distX, distY);
    }

    
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        this.setDragging(false);
        return getFocused() != null && getFocused().mouseReleased(mouseX, mouseY, button);
    }

    
    public boolean isObstructed(Window<?> window, double mouseX, double mouseY)
    {
        for(Window<?> window1 : getEventListeners())
        {
            if(Fragment.isMouseBetween(mouseX, window1.getLeft(), window1.getLeft() + window1.width) && Fragment.isMouseBetween(mouseY, window1.getTop(), window1.getTop() + window1.height))
            {
                return window != window1;
            }
        }
        return true; //our window isn't even here! pretend we're obstructed
    }

    public <T extends Window<?>> T getByWindowType(Class<T> clz)
    {
        List<Window<?>> windows = getEventListeners();
        for(Window<?> window : windows)
        {
            if(clz.isAssignableFrom(window.getClass()))
            {
                return (T)window;
            }
        }
        return null;
    }

    
    public boolean canDockWindows()
    {
        return true;
    }

    public WindowDock<? extends Workspace> getDock()
    {
        return (WindowDock<? extends Workspace>)windows.get(windows.size() - 1);
    }

    
    public DockInfo getDockInfo(double mouseX, double mouseY, boolean dockStack)
    {
        if(canDockWindows())
        {
            return getDock().getDockInfo(mouseX, mouseY, dockStack);
        }
        return null;
    }

    
    public void addToDocked(Window<?> docked, Window<?> window)
    {
        if(canDockWindows() && getDock().addToDocked(docked, window))
        {
            removeWindow(window);
        }
    }

    
    public void addToDock(Window<?> window, Constraint.Property.Type type)
    {
        if(canDockWindows())
        {
            getDock().addToDock(window, type);
            removeWindow(window);
        }
    }

    
    public void removeFromDock(Window<?> window)
    {
        if(canDockWindows())
        {
            getDock().removeFromDock(window);
            addWindow(window);
        }
    }

    
    public boolean isDocked(Window<?> window)
    {
        if(canDockWindows())
        {
            return getDock().isDocked(window);
        }
        return false;
    }

    
    public boolean sameDockStack(IConstrainable window, IConstrainable window1)
    {
        if(canDockWindows())
        {
            return getDock().sameDockStack(window, window1);
        }
        return false;
    }

    
    public void setFocused(@Nullable GuiEventListener gui)
    {
        GuiEventListener lastFocused = getFocused();
        if(lastFocused instanceof Fragment && gui != lastFocused)
        {
            ((Fragment<?>)lastFocused).unfocus(gui);
        }
        if(gui instanceof Window)
        {
            bringToFront((Window<?>)gui);
        }
        super.setFocused(gui);
    }

    //IConstrainable
    
    public int getLeft()
    {
        return 0;
    }

    
    public int getRight()
    {
        return width;
    }

    
    public int getTop()
    {
        return 0;
    }

    
    public int getBottom()
    {
        return height;
    }


    //Convenience method
    public static void bindTexture(ResourceLocation rl)
    {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, rl);
    }
}
