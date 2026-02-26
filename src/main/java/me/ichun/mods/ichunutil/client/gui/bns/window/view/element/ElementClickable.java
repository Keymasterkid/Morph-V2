package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import net.minecraft.client.gui.GuiGraphics;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public abstract class ElementClickable<T extends ElementClickable> extends Element //we reset our focus when we're clicked.
{
    public @Nonnull Consumer<T> callback;
    public boolean hover; //for rendering
    public boolean disabled;

    public ElementClickable(@Nonnull Fragment parent, Consumer<T> callback)
    {
        super(parent);
        this.callback = callback;
    }

    public <T extends ElementClickable<?>> T setDisabled(boolean flag)
    {
        disabled = flag;
        return (T)this;
    }

    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        hover = false;
        if(!disabled)
        {
            hover = isMouseOver(mouseX, mouseY) || parentFragment.getFocused() == this;
        }
    }

    
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        boolean flag = super.mouseReleased(mouseX, mouseY, button); // unsets dragging;
        parentFragment.setFocused(null); //we're a one time click, stop focusing on us
        if(!disabled && isMouseOver(mouseX, mouseY) && button == 0) //lmb
        {
            trigger();
        }
        return flag;
    }

    public void trigger()
    {
        if(renderMinecraftStyle() > 0)
        {
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        onClickRelease();
        callback.accept((T)this);
    }

    public abstract void onClickRelease();

    
    public boolean keyPressed(int key, int scancode, int listener)
    {
        if(!disabled && (key == com.mojang.blaze3d.platform.InputConstants.KEY_SPACE || key == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || key == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER))
        {
            trigger();
            return true;
        }
        return false;
    }

    
    public int getMinecraftFontColour()
    {
        return parentFragment.isDragging() && parentFragment.getFocused() == this || disabled ? 10526880 : hover ? 16777120 : 14737632;
    }
}
