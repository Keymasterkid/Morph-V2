package me.ichun.mods.morph.client.gui.biomass.window.element;

import net.minecraft.client.gui.GuiGraphics;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.element.Element;
import me.ichun.mods.morph.client.gui.biomass.window.WindowHeader;
import me.ichun.mods.morph.common.Morph;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ElementBiomassBar extends Element<WindowHeader.ViewHeader>
{
    public ElementBiomassBar(@Nonnull WindowHeader.ViewHeader parent)
    {
        super(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int x = getLeft();
        int y = getTop() + (int)((getHeight() - 5) / 2D);

        Morph.eventHandlerClient.hudHandler.drawBiomassBar(guiGraphics, x, y, partialTick, 1F);
    }

    @Nullable
    @Override
    public String tooltip(double mouseX, double mouseY)
    {
        return super.tooltip(mouseX, mouseY); //TODO this
    }

    @Override
    public int getMinWidth()
    {
        return 182;
    }

    @Override
    public int getMinHeight()
    {
        return 5;
    }

    @Override
    public int getMaxWidth()
    {
        return 182;
    }

    @Override
    public int getMaxHeight()
    {
        return super.getMaxHeight();
    }
}
