package me.ichun.mods.ichunutil.common.core;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class EventHandlerServer
{
    public int ticks;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event)
    {
        ticks++;
    }
}
