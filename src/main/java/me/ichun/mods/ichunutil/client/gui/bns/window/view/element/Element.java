package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unchecked")
public abstract class Element<P extends Fragment> extends Fragment<P> //TODO handle narration?
{
    public final static List<Element<?>> INFERTILE = Collections.emptyList();

    public String tooltip;

    public Element(@Nonnull P parent)
    {
        super(parent);
    }

    public <T extends Element<?>> T setPos(int x, int y)
    {
        posX = x;
        posY = y;
        return (T)this;
    }

    public <T extends Element<?>> T setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
        return (T)this;
    }

    public <T extends Element<?>> T setTooltip(String s)
    {
        tooltip = s;
        return (T)this;
    }

    
    public @Nullable String tooltip(double mouseX, double mouseY)
    {
        return tooltip;
    }

    
    public void init()
    {
        constraint.apply();
    }

    
    public List<? extends Fragment<?>> getEventListeners()
    {
        return INFERTILE;
    }

    
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick){}

    
    public void resize(Minecraft mc, int width, int height)
    {
        constraint.apply();
    }

    
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        return isMouseOver(mouseX, mouseY);
    }

    /**
     * This is here to bypass our mouseClicked and use Fragment's
     */
    public boolean defaultMouseClicked(double mouseX, double mouseY, int button)
    {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    
    public boolean changeFocus(boolean direction)
    {
        return parentFragment.getFocused() != this; //focus on us if we're not focused
    }

    public enum ButtonState
    {
        IDLE,
        HOVER,
        CLICK
    }

    public static void renderMinecraftStyleButton(GuiGraphics guiGraphics, int posX, int posY, int width, int height, ButtonState state, int minecraftStyle) // BUTTONS NEED TO BE LARGER THAN 3x3
    {
        // Bind texture and render using GuiGraphics
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, minecraftStyle == 2 ? VANILLA_WIDGETS : WIDGETS, posX, posY, 0F, state == ButtonState.CLICK ? 0F : state == ButtonState.HOVER ? 40F : 20F, width, height, 256, 256);
    }

    public static void cropAndStitch(GuiGraphics guiGraphics, int posX, int posY, int width, int height, int borderSize, double u, double v, int uLength, int vLength, double texWidth, double texHeight)
    {
        RenderHelper.startDrawBatch();
        int jj = height - (borderSize * 2);
        int yy = posY + borderSize;
        int distx = width - (borderSize * 2);
        while(jj > 0)
        {
            int disty = Math.min(jj, vLength - (borderSize * 2));
            RenderHelper.drawBatch(guiGraphics, posX + borderSize, yy, distx, disty, 0, (u + borderSize)/texWidth, (v + borderSize)/texHeight, distx/texWidth, disty/texHeight); //draw body
            jj -= disty;
            yy += disty;
        }

        int i = width - (borderSize * 2);
        int x = posX + borderSize;
        while(i > 0)
        {
            int dist = Math.min(i, uLength - (borderSize * 2));
            RenderHelper.drawBatch(guiGraphics, x, posY, dist, borderSize, 0, (u + borderSize)/texWidth, v/texHeight, dist/texWidth, borderSize/texHeight); //draw top bar
            RenderHelper.drawBatch(guiGraphics, x, posY + height - borderSize, dist, borderSize, 0, (u + borderSize)/texWidth, (v + vLength - borderSize)/texHeight, dist/texWidth, borderSize/texHeight); //draw bottom bar
            i -= dist;
            x += dist;
        }

        i = height - (borderSize * 2);
        x = posY + borderSize;
        while(i > 0)
        {
            int dist = Math.min(i, vLength - (borderSize * 2));
            RenderHelper.drawBatch(guiGraphics, posX, x, borderSize, dist, 0, u/texWidth, (v + borderSize)/texHeight, borderSize/texWidth, dist/texHeight); //draw left bar
            RenderHelper.drawBatch(guiGraphics, posX + width - borderSize, x, borderSize, dist, 0, (u + uLength - borderSize)/texWidth, (v + borderSize)/texHeight, borderSize/texWidth, dist/texHeight); //draw right bar
            i -= dist;
            x += dist;
        }

        RenderHelper.drawBatch(guiGraphics, posX, posY + height - borderSize, borderSize, borderSize, 0, u/texWidth, (v + vLength - borderSize)/texHeight, borderSize/texWidth, borderSize/texHeight); //draw bottomLeft
        RenderHelper.drawBatch(guiGraphics, posX, posY, borderSize, borderSize, 0, u/texWidth, v/texHeight, borderSize/texWidth, borderSize/texHeight); //draw topLeft
        RenderHelper.drawBatch(guiGraphics, posX + width - borderSize, posY, borderSize, borderSize, 0, (u + uLength - borderSize)/texWidth, v/texHeight, borderSize/texWidth, borderSize/texHeight); //draw topRight
        RenderHelper.drawBatch(guiGraphics, posX + width - borderSize, posY + height - borderSize, borderSize, borderSize, 0, (u + uLength - borderSize)/texWidth, (v + vLength - borderSize)/texHeight, borderSize/texWidth, borderSize/texHeight); //draw bottomRight

        RenderHelper.endDrawBatch();
    }

    public static class MousePos
    {
        public int x;
        public int y;

        public MousePos(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }
}
