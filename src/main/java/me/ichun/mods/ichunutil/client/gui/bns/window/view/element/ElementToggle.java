package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.Theme;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ElementToggle<T extends ElementToggle> extends ElementClickable<T>
{
    public String text;
    public boolean toggleState;

    public ElementToggle(@Nonnull Fragment parent, @Nonnull String s, Consumer<T> callback)
    {
        super(parent, callback);
        text = !s.isEmpty() ? I18n.get(s) : "";
    }

    public <T extends ElementToggle<?>> T setToggled(boolean flag)
    {
        toggleState = flag;
        return (T)this;
    }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if(renderMinecraftStyle() > 0)
        {
            renderMinecraftStyleButton(guiGraphics, getLeft(), getTop(), width, height, disabled || parentFragment.isDragging() && parentFragment.getFocused() == this || toggleState ? ButtonState.CLICK : hover ? ButtonState.HOVER : ButtonState.IDLE, renderMinecraftStyle());
        }
        else
        {
            fill(guiGraphics, getTheme().elementButtonBorder, 0);
            int[] colour;
            if(disabled)
            {
                colour = getTheme().elementButtonBackgroundInactive;
            }
            else if(parentFragment.isDragging() && parentFragment.getFocused() == this)
            {
                colour = getTheme().elementButtonClick;
            }
            else if(toggleState && hover)
            {
                colour = getTheme().elementButtonToggleHover;
            }
            else if(hover)
            {
                colour = getTheme().elementButtonBackgroundHover;
            }
            else if(toggleState)
            {
                colour = getTheme().elementButtonToggle;
            }
            else
            {
                colour = getTheme().elementButtonBackgroundInactive;
            }
            fill(guiGraphics, colour, 1);
        }
        renderText(guiGraphics);
    }

    public void renderText(GuiGraphics guiGraphics)
    {
        if(!text.isEmpty())
        {
            String s = reString(text, width - 4);

            //draw the text
            if(renderMinecraftStyle() > 0)
            {
                // drawStringWithShadow stubbed
                drawString(guiGraphics, s, getLeft() + 2, getTop() + (height - net.minecraft.client.Minecraft.getInstance().font.lineHeight) / 2F + 1);
            }
            else
            {
                // drawString stubbed
                drawString(guiGraphics, s, getLeft() + 2, getTop() + (height - net.minecraft.client.Minecraft.getInstance().font.lineHeight) / 2F + 1);
            }
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

    
    public void onClickRelease()
    {
        toggleState = !toggleState;
    }

    
    public int getMinWidth()
    {
        return 14;
    }

    
    public int getMinHeight()
    {
        return 14;
    }
}
