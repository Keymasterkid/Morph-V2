package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
// NetworkEvent removed - use IPayload pattern in 1.21

public class PacketMorphInput extends AbstractPacket
{
    public String identifier;
    public boolean inputFavourite;
    public boolean isFavourite;
    public boolean isDelete;

    public PacketMorphInput(){}

    public PacketMorphInput(String identifier, boolean inputFavourite, boolean isFavourite, boolean isDelete)
    {
        this.identifier = identifier;
        this.inputFavourite = inputFavourite;
        this.isFavourite = isFavourite;
        this.isDelete = isDelete;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeUtf(identifier);
        buf.writeBoolean(inputFavourite);
        buf.writeBoolean(isFavourite);
        buf.writeBoolean(isDelete);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        identifier = readString(buf);
        inputFavourite = buf.readBoolean();
        isFavourite = buf.readBoolean();
        isDelete = buf.readBoolean();
    }

    @Override
    public void process(net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        context.enqueueWork(() -> {
            PlayerMorphData morphData = MorphHandler.INSTANCE.getPlayerMorphData(context.player());
            for(MorphVariant morph : morphData.morphs)
            {
                MorphVariant.Variant variant = morph.getVariantById(identifier);
                if(variant != null)
                {
                    if(inputFavourite)
                    {
                        variant.isFavourite = isFavourite;

                        MorphHandler.INSTANCE.getSaveData().setDirty();
                    }
                    else if(isDelete)
                    {
                        if(MorphHandler.INSTANCE.getMorphModeName().equals("classic") && morph.removeVariant(variant))
                        {
                            MorphHandler.INSTANCE.getSaveData().setDirty();

                            Morph.channel.sendTo(new PacketUpdateMorph(morph.write(new CompoundTag())), (net.minecraft.server.level.ServerPlayer)context.player());
                        }
                    }
                    else
                    {
                        if(MorphHandler.INSTANCE.canMorph(context.player()))
                        {
                            MorphHandler.INSTANCE.morphTo((net.minecraft.server.level.ServerPlayer)context.player(), morph.getAsVariant(variant));
                        }
                    }

                    return;
                }
            }
        });
    }
}
