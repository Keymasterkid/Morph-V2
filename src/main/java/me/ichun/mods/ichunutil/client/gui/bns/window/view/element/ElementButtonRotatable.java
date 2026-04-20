package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import net.minecraft.client.gui.GuiGraphics;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ElementButtonRotatable<T extends ElementButtonRotatable> extends ElementButton<T>
{
    public int rotationCount;

    public ElementButtonRotatable(@Nonnull Fragment parent, @Nonnull String s, int rotCount, Consumer<T> callback)
    {
        super(parent, s, callback);
        this.rotationCount = rotCount;
    }

    
    public Element<?> setSize(int width, int height)
    {
        if(rotationCount % 2 != 0)
        {
            return super.setSize(height, width); //flip them
        }
        return super.setSize(width, height);
    }

    
    @Override
    public void renderText(GuiGraphics guiGraphics)
    {
        if(!text.isEmpty())
        {
            String s = reString(text, (rotationCount % 2 != 0 ? height : width) - 4);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(getLeft() + (width / 2F)), (float)(getTop() + (height / 2F)));
            guiGraphics.pose().rotate((float) Math.toRadians(90F * rotationCount));
            guiGraphics.pose().translate((float)(- net.minecraft.client.Minecraft.getInstance().font.width(s) / 2F), (float)(- (net.minecraft.client.Minecraft.getInstance().font.lineHeight) / 2F + 1));

            //draw the text
            drawString(guiGraphics, s, 0, 0);
            guiGraphics.pose().popMatrix();
        }
    }

    @Nullable
    
    public String tooltip(double mouseX, double mouseY)
    {
        if(!text.isEmpty())
        {
            String s = reString(text, (rotationCount % 2 != 0 ? height : width) - 4);
            if(!s.equals(text))
            {
                if(tooltip != null)
                {
                    return text + " - " + tooltip;
                }
                return text;
            }
        }
        return tooltip;
    }
}
