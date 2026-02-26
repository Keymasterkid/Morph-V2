package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;

import javax.annotation.Nonnull;

public class ElementPadding extends Element
{
    public int minWidth;
    public int minHeight;

    public ElementPadding(@Nonnull Fragment parent, int minWidth, int minHeight)
    {
        super(parent);
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return false;
    }

    
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        return false;
    }

    
    public boolean changeFocus(boolean direction)
    {
        return false;
    }

    
    public int getMinWidth()
    {
        return minWidth;
    }

    
    public int getMinHeight()
    {
        return minHeight;
    }

}
