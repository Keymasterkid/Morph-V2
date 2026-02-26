package me.ichun.mods.morph.api.event;

import me.ichun.mods.morph.api.morph.MorphVariant;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.ICancellableEvent;

public class MorphEvent extends PlayerEvent 
{
    private final MorphVariant variant;
    private MorphEvent(Player player, MorphVariant variant)
    {
        super(player);
        this.variant = variant;
    }

    public MorphVariant getVariant()
    {
        return variant;
    }

    public static class CanAcquire extends MorphEvent implements ICancellableEvent 
    {
        public CanAcquire(Player player, MorphVariant variant)
        {
            super(player, variant);
        }
    }

    public static class Acquire extends MorphEvent implements ICancellableEvent 
    {
        public Acquire(Player player, MorphVariant variant)
        {
            super(player, variant);
        }
    }

    public static class Morph extends MorphEvent implements ICancellableEvent 
    {
        public Morph(Player player, MorphVariant variant)
        {
            super(player, variant);
        }
    }
}
