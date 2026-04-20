package me.ichun.mods.ichunutil.client.gui.bns.window.view;

import net.minecraft.client.gui.GuiGraphics;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import me.ichun.mods.ichunutil.client.gui.bns.window.IWindows;
import me.ichun.mods.ichunutil.client.gui.bns.window.Window;
import me.ichun.mods.ichunutil.client.gui.bns.window.constraint.Constraint;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.element.Element;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public abstract class View<P extends Window<? extends IWindows>> extends Fragment<P>
{
    public ArrayList<Element<?>> elements = new ArrayList<>();
    public @Nonnull String title; // we localise when this is set

    public View(@Nonnull P parent, @Nonnull String s)
    {
        super(parent);
        title = I18n.get(s);
        constraint = Constraint.matchParent(this, parent, parent.borderSize.get());
        if(parent.canShowTitle() && !s.isEmpty())
        {
            constraint.top(parent, Constraint.Property.Type.TOP, parent.titleSize.get());
        }
    }

    public <T extends View<?>> T setPos(int x, int y)
    {
        posX = x;
        posY = y;
        return (T)this;
    }

    public <T extends View<?>> T setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
        return (T)this;
    }

    
    public void init()
    {
        constraint.apply();
        elements.forEach(Fragment::init);
    }

    
    public List<Element<?>> getEventListeners()
    {
        return elements;
    }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        setScissor(guiGraphics);
        //render our background
        renderBackground(guiGraphics);

        //render attached elements
        for(Element<?> element : elements)
        {
            element.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        resetScissorToParent(guiGraphics);
    }

    public void renderBackground(GuiGraphics guiGraphics)
    {
        if(renderMinecraftStyle() == 0)
        {
            fill(guiGraphics, getTheme().windowBackground, 255, 0);
        }
    }

    
    public void resize(Minecraft mc, int width, int height)
    {
        constraint.apply();
        elements.forEach(element -> element.resize(mc, this.width, this.height));
    }

    
    public boolean changeFocus(boolean direction)
    {
        if(parentFragment.getFocused() == this)
        {
            boolean flag = false;
            if(!flag)
            {
                flag = false;
            }
            return flag;
        }
        return false; //we're not focused anyway, so, nah
    }

    
    public boolean requireScissor()
    {
        return true;
    }
}
