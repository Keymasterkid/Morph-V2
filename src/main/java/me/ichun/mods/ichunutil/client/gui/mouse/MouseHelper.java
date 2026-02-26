package me.ichun.mods.ichunutil.client.gui.mouse;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

public class MouseHelper
{
    public static double getMouseDistanceFromCenter(Window window)
    {
        double centerX = window.getGuiScaledWidth() / 2D;
        double centerY = window.getGuiScaledHeight() / 2D;

        Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        double posX = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getWidth() - centerX;
        double posY = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getHeight() - centerY;

        return Math.sqrt(posX * posX + posY * posY);
    }

    public static float getMouseAngleFromCenter(Window window)
    {
        double centerX = window.getGuiScaledWidth() / 2D;
        double centerY = window.getGuiScaledHeight() / 2D;

        Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        double posX = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getWidth() - centerX;
        double posY = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getHeight() - centerY;

        return (float)(Math.toDegrees(Math.atan2(posY, posX)) + 90F + 360F) % 360F;
    }

    public static int getSelectedIndex(int count)
    {
        float angle = getMouseAngleFromCenter(net.minecraft.client.Minecraft.getInstance().getWindow());

        float segment = 360F / count;

        float startSeg = segment / 2F;

        for(int i = 0; i < count; i++)
        {
            if(angle < startSeg)
            {
                return i;
            }
            startSeg += segment;
        }

        return 0; //angle is larger than (360 - (segment / 2)), it's back to index of 0
    }
}
