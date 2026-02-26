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
            handleClient();
        }
    }

    private void handleClient()
    {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        
        if(targetId >= 0)
        {
            Entity target = mc.level.getEntity(targetId);

            if(target instanceof LivingEntity && !(target instanceof Player))
            {
                LivingEntity living = (LivingEntity)target;

                mc.setScreen(new me.ichun.mods.morph.client.gui.nbt.WorkspaceNbt(mc.screen, living));
            }
        }
        else
        {
            mc.setScreen(new me.ichun.mods.morph.client.gui.mob.WorkspaceMobData(mc.screen));
        }
    }
}
