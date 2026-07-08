package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.mob.trait.ability.Ability;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketAbility extends AbstractPacket
{
    public boolean isDown;

    public PacketAbility() {}

    public PacketAbility(boolean isDown)
    {
        this.isDown = isDown;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeBoolean(isDown);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        isDown = buf.readBoolean();
    }

    @Override
    public void process(IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if(context.player() instanceof ServerPlayer)
            {
                ServerPlayer player = (ServerPlayer)context.player();
                MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(player);
                if(info != null && info.isMorphed() && info.nextState != null)
                {
                    for(Trait<?> trait : info.nextState.traits)
                    {
                        if(trait.isAbility())
                        {
                            Ability<?> ability = (Ability<?>)trait;
                            if(ability.isActive() && MorphApi.getApiImpl().canUseAbility(player, ability))
                            {
                                ability.onAction(isDown);
                            }
                        }
                    }
                }
            }
        });
    }
}
