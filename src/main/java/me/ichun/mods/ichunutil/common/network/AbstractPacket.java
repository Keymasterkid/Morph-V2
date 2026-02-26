package me.ichun.mods.ichunutil.common.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Base class for mod packets. In NeoForge 1.21, packets are sent via the
 * IPayload system. This class is kept as a data holder; actual registration
 * happens in PacketChannel.
 */
public abstract class AbstractPacket
{
    public abstract void writeTo(FriendlyByteBuf buf);
    public abstract void readFrom(FriendlyByteBuf buf);
    /** Called on the receiver side (may be on networking thread). Enqueue to main thread as needed. */
    public abstract void process(net.neoforged.neoforge.network.handling.IPayloadContext context);
    public final String readString(FriendlyByteBuf buf) { return buf.readUtf(32767); }
}
