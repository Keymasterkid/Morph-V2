package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import net.minecraft.network.FriendlyByteBuf;

public class PacketInvalidateClientHealth extends AbstractPacket
{
    public PacketInvalidateClientHealth(){}

    @Override
    public void writeTo(FriendlyByteBuf buf){}

    @Override
    public void readFrom(FriendlyByteBuf buf){}

    @Override
    public void process(net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (context.flow().isClientbound()) {
            context.enqueueWork(this::handleClient);
        }
    }

    private void handleClient()
    {
        // net.minecraft.client.Minecraft.getInstance().player.hasValidHealth = false;
    }
}
