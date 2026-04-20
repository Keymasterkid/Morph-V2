package me.ichun.mods.ichunutil.client.core;

import me.ichun.mods.ichunutil.common.iChunUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

public class ClientSetup {
    public static ConfigClient configClient;
    public static EventHandlerClient eventHandlerClient;

    private static boolean listenerRegistered = false;
    public static void init(IEventBus bus, ModContainer modContainer) {
        ResourceHelper.init();
        configClient = new ConfigClient().init();
        NeoForge.EVENT_BUS.register(eventHandlerClient = new EventHandlerClient());

        if (!listenerRegistered) {
            bus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) -> {
                iChunUtil.LOGGER.info("Registering {} key mappings for iChunUtil", me.ichun.mods.ichunutil.client.key.KeyBind.KEY_MAPPINGS.size());
                me.ichun.mods.ichunutil.client.key.KeyBind.KEY_MAPPINGS.forEach(event::register);
            });
            listenerRegistered = true;
        }

        modContainer.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (mc, parent) -> new me.ichun.mods.ichunutil.client.gui.config.WorkspaceConfigs(parent));
    }
}
