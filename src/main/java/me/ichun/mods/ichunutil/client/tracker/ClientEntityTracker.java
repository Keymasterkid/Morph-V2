package me.ichun.mods.ichunutil.client.tracker;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import java.util.HashMap;
public class ClientEntityTracker {
    public static final HashMap<net.minecraft.world.entity.Entity, me.ichun.mods.ichunutil.client.tracker.entity.EntityTracker> TRACKERS = new HashMap<>();
    public static class EntityTypes {
        public static EntityType<me.ichun.mods.ichunutil.client.tracker.entity.EntityTracker> TRACKER;
    }
    public static void attachClient(IEventBus bus) {}

    private static int nextEntId = -1000000;
    public static int getNextEntId()
    {
        return nextEntId--;
    }
}
