package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
// NetworkEvent removed - use IPayload pattern in 1.21

public class PacketUpdateMorph extends AbstractPacket //Only used for the full list of variants. Singular morphs should not use this!
{
    public CompoundTag nbt;

    public PacketUpdateMorph(){}

    public PacketUpdateMorph(CompoundTag nbt)
    {
        this.nbt = nbt;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeNbt(nbt);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        nbt = buf.readNbt();
    }

    @Override
    public void process(net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> me.ichun.mods.morph.client.network.ClientPayloadHandler.handlePacketUpdateMorph(this, context));
        }
    }
}
