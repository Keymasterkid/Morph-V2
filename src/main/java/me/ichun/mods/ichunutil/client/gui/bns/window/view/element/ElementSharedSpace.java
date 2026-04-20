package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import net.minecraft.client.gui.GuiGraphics;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ElementSharedSpace extends ElementFertile
{
    public final ElementScrollBar.Orientation orientation;
    public List<Element<?>> elements = new ArrayList<>();

    public ElementSharedSpace(@Nonnull Fragment parent, ElementScrollBar.Orientation orientation)
    {
        super(parent);
        this.orientation = orientation;
    }

    public ElementSharedSpace addElement(Element<?> e)
    {
        elements.add(e);
        return this;
    }

    
    public List<? extends Fragment<?>> getEventListeners()
    {
        return elements;
    }

    
    public int getBorderSize()
    {
        return 0;
    }

    
    public void init()
    {
        constraint.apply();
        updateSizes();
        elements.forEach(Element::init);
    }

    
    public void resize(Minecraft mc, int width, int height)
    {
        constraint.apply();
        updateSizes();
        elements.forEach(e -> e.resize(mc, width, height));
    }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        setScissor(guiGraphics);
        elements.forEach(e -> e.render(guiGraphics, mouseX, mouseY, partialTick));
        resetScissorToParent(guiGraphics);
    }

    
    public int getMinWidth()
    {
        if(orientation == ElementScrollBar.Orientation.HORIZONTAL)
        {
            return 8;
        }
        return super.getMinWidth();
    }

    
    public int getMinHeight()
    {
        if(orientation == ElementScrollBar.Orientation.VERTICAL)
        {
            return 8;
        }
        return super.getMinHeight();
    }

    public void updateSizes()
    {
        int size = (orientation == ElementScrollBar.Orientation.HORIZONTAL ? width : height) / elements.size();
        for(Element<?> element : elements)
        {
            if(orientation == ElementScrollBar.Orientation.HORIZONTAL)
            {
                element.setWidth(size);
            }
            else
            {
                element.setHeight(size);
            }
        }
    }


}
