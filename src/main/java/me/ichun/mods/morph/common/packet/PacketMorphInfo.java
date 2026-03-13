package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class PacketMorphInfo extends AbstractPacket
{
    public int entId;
    public CompoundTag nbt;

    public PacketMorphInfo(){}

    public PacketMorphInfo(int id, CompoundTag nbt)
    {
        this.entId = id;
        this.nbt = nbt;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeInt(entId);
        buf.writeNbt(nbt);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        entId = buf.readInt();
        nbt = buf.readNbt();
    }

    @Override
    public void process(net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> me.ichun.mods.morph.client.network.ClientPayloadHandler.handlePacketMorphInfo(this, context));
        }
    }
}
