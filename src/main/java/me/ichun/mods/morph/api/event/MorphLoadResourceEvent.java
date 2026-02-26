package me.ichun.mods.morph.api.event;

import net.neoforged.bus.api.Event;

public class MorphLoadResourceEvent extends Event
{
    public enum Type
    {
        BIOMASS,
        InteractionHand,
        MOB,
        NBT
    }

    private final Type type;

    public MorphLoadResourceEvent(Type type)
    {
        this.type = type;
    }

    public Type getType()
    {
        return type;
    }
}
