package me.ichun.mods.ichunutil.common;

import me.ichun.mods.ichunutil.common.core.EventHandlerServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@net.neoforged.fml.common.Mod(iChunUtil.MOD_ID)
public class iChunUtil
{
    public static final String MOD_ID = "ichunutil";
    public static final String MOD_NAME = "iChunUtil";
    public static final Logger LOGGER = LogManager.getLogger();
    public static EventHandlerServer eventHandlerServer;

    public iChunUtil(net.neoforged.bus.api.IEventBus bus, net.neoforged.fml.ModContainer modContainer)
    {
        me.ichun.mods.ichunutil.common.util.ObfHelper.detectDevEnvironment();
        me.ichun.mods.ichunutil.common.util.EventCalendar.checkDate();

        for (java.lang.reflect.Field f : modContainer.getClass().getDeclaredFields()) {
            LOGGER.info("ModContainer field: {}", f.getName());
        }

        bus.addListener(this::setup);
        bus.addListener(this::processIMC);
        bus.addListener(this::finishLoading);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(eventHandlerServer = new EventHandlerServer());

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            me.ichun.mods.ichunutil.client.core.ClientSetup.init(bus, modContainer);
        }
    }

    private void setup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)
    {
    }

    private void processIMC(net.neoforged.fml.event.lifecycle.InterModProcessEvent event)
    {
    }

    private void finishLoading(net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent event)
    {
    }
}
