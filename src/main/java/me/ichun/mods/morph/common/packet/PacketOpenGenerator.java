package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

public class PacketOpenGenerator extends AbstractPacket
{
    public int targetId;

    public PacketOpenGenerator(){}

    public PacketOpenGenerator(int targetId)
    {
        this.targetId = targetId;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeInt(targetId);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        targetId = buf.readInt();
    }

    @Override
    public void process(net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> me.ichun.mods.morph.client.network.ClientPayloadHandler.handlePacketOpenGenerator(this, context));
        }
    }
}
