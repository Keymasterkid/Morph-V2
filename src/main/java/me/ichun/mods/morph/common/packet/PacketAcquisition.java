package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.FriendlyByteBuf;

public class PacketAcquisition extends AbstractPacket
{
    public int originId;
    public int acquiredId;

    public boolean isMorphAcquisition;

    public PacketAcquisition(){}

    public PacketAcquisition(int originId, int acquiredId, boolean isMorphAcquisition)
    {
        this.originId = originId;
        this.acquiredId = acquiredId;
        this.isMorphAcquisition = isMorphAcquisition;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeInt(originId);
        buf.writeInt(acquiredId);
        buf.writeBoolean(isMorphAcquisition);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        originId = buf.readInt();
        acquiredId = buf.readInt();
        isMorphAcquisition = buf.readBoolean();
    }

    @Override
    public void process(net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if(isMorphAcquisition && (Morph.configClient.acquisitionPlayAnimation == 1 || Morph.configClient.acquisitionPlayAnimation == 3)|| !isMorphAcquisition && Morph.configClient.acquisitionPlayAnimation >= 2)
        {
            if (context.flow().isClientbound()) {
                context.enqueueWork(() -> me.ichun.mods.morph.client.network.ClientPayloadHandler.handlePacketAcquisition(this, context));
            }
        }
    }
}
