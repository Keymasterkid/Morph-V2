package me.ichun.mods.ichunutil.client.core;

import me.ichun.mods.ichunutil.client.gui.config.WorkspaceConfigs;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.common.iChunUtil;
import me.ichun.mods.ichunutil.common.util.ObfHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class EventHandlerClient
{
    public static Screen getConfigGui(Minecraft mc, Screen parentScreen) { return new WorkspaceConfigs(parentScreen); } //for mod config compat

    public int ticks;

    public float partialTick;

    public int screenWidth;
    public int screenHeight;

    @SubscribeEvent
    public void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event)
    {
        if(false)
        {
            ticks++;
        }
    }

    @SubscribeEvent
    public void onRenderTick(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event)
    {
        Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        partialTick = event.getPartialTick().getGameTimeDeltaTicks();

        if(screenWidth != mc.getWindow().getWidth() || screenHeight != mc.getWindow().getHeight())
        {
            screenWidth = mc.getWindow().getWidth();
            screenHeight = mc.getWindow().getHeight();

            for(RenderTarget buffer : RenderHelper.frameBuffers)
            {
                buffer.resize(screenWidth, screenHeight, Minecraft.ON_OSX);
            }
        }
    }

    // disabled event
    public void onGuiInit(ScreenEvent.Init.Post event)
    {
    }
}