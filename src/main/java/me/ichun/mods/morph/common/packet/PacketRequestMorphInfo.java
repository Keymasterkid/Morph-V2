package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
// NetworkEvent removed - use IPayload pattern in 1.21

import java.util.UUID;

public class PacketRequestMorphInfo extends AbstractPacket
{
    public UUID playerId;

    public PacketRequestMorphInfo(){}

    public PacketRequestMorphInfo(UUID id)
    {
        playerId = id;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeUUID(playerId);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        playerId = buf.readUUID();
    }

    @Override
    public void process(net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        context.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer sender = (net.minecraft.server.level.ServerPlayer) context.player();
            Player player = sender.getServer().getPlayerList().getPlayer(playerId);
            if(player != null && !player.isRemoved())
            {
                Morph.channel.sendTo(new PacketMorphInfo(player.getId(), MorphHandler.INSTANCE.getMorphInfo(player).write(new CompoundTag())), sender);
            }
        });
    }
}
