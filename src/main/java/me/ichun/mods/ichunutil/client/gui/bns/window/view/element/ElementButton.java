package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import net.minecraft.client.gui.GuiGraphics;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ElementButton<T extends ElementButton> extends ElementClickable<T>
{
    public @Nonnull String text;
    public boolean renderBackground = true;

    public ElementButton(@Nonnull Fragment parent, String s, Consumer<T> callback)
    {
        super(parent, callback);
        String translated = I18n.get(s);
        if (translated == null || translated.isEmpty() || translated.equals(s)) {
            if (s.equals("selectWorld.edit")) translated = "Edit";
            else translated = s;
        }
        text = translated;
    }

    public ElementButton<T> disableBackground()
    {
        renderBackground = false;
        return this;
    }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if(renderBackground)
        {
            if(renderMinecraftStyle() > 0)
            {
                renderMinecraftStyleButton(guiGraphics, getLeft(), getTop(), width, height, disabled || parentFragment.isDragging() && parentFragment.getFocused() == this ? ButtonState.CLICK : hover ? ButtonState.HOVER : ButtonState.IDLE, renderMinecraftStyle());
            }
            else
            {
                fill(guiGraphics, getTheme().elementButtonBorder, 255, 0);
                int[] colour = disabled ? getTheme().elementButtonBackgroundInactive : parentFragment.isDragging() && parentFragment.getFocused() == this ? getTheme().elementButtonClick : hover ? getTheme().elementButtonBackgroundHover : getTheme().elementButtonBackgroundInactive;
                fill(guiGraphics, colour, 255, 1);
            }
        }
        renderText(guiGraphics);
    }

    public void renderText(GuiGraphics guiGraphics)
    {
        if(!text.isEmpty())
        {
            String s = reString(text, width - 4);
            drawString(guiGraphics, s, getLeft() + (this.width - net.minecraft.client.Minecraft.getInstance().font.width(s)) / 2F, getTop() + (height - net.minecraft.client.Minecraft.getInstance().font.lineHeight) / 2F + 1);
        }
    }

    @Nullable
    
    public String tooltip(double mouseX, double mouseY)
    {
        if(!text.isEmpty())
        {
            String s = reString(text, width - 4);
            if(!s.equals(text))
            {
                String tooltip = super.tooltip(mouseX, mouseY);
                if(tooltip != null)
                {
                    return text + " - " + tooltip;
                }
                return text;
            }
        }
        return super.tooltip(mouseX, mouseY);
    }

    
    public void onClickRelease() {} //we don't do anything, we're a static button

    
    public int getMinWidth()
    {
        return 14;
    }

    
    public int getMinHeight()
    {
        return 14;
    }
}
