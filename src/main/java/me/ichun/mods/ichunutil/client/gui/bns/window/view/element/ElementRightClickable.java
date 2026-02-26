package me.ichun.mods.ichunutil.client.gui.bns.window.view.element;

import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public abstract class ElementRightClickable<T extends ElementRightClickable> extends ElementClickable<T> //we reset our focus when we're clicked.
{
    public @Nonnull Consumer<T> rightClickCallback;

    public ElementRightClickable(@Nonnull Fragment parent, Consumer<T> callback, Consumer<T> rightClickCallback)
    {
        super(parent, callback);
        this.rightClickCallback = rightClickCallback;
    }

    
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
//        boolean flag = super.mouseReleased(mouseX, mouseY, button); // unsets dragging;
        //copied out mouseReleased so we don't call ElementClickable's
        this.setDragging(false);
        boolean flag = getFocused() != null && getFocused().mouseReleased(mouseX, mouseY, button);

        parentFragment.setFocused(null); //we're a one time click, stop focusing on us
        if(!disabled && isMouseOver(mouseX, mouseY))
        {
            if(button == 0)
            {
                trigger();
            }
            else if(button == 1)
            {
                triggerRMB();
            }
        }
        return flag;
    }

    public void triggerRMB()
    {
        if(renderMinecraftStyle() > 0)
        {
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        onRightClickRelease();
        rightClickCallback.accept((T)this);
    }

    public abstract void onRightClickRelease();

    
    public boolean keyPressed(int key, int scancode, int listener)
    {
        if(!disabled && Screen.hasControlDown() && (key == com.mojang.blaze3d.platform.InputConstants.KEY_SPACE || key == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || key == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER))
        {
            triggerRMB();
            return true;
        }
        return super.keyPressed(key, scancode, listener);
    }
}
