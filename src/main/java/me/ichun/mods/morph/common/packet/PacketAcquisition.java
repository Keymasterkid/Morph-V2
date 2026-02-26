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
                context.enqueueWork(() -> handleClient());
            }
        }
    }

    private void handleClient()
    {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        
        Entity origin = mc.level.getEntity(originId);
        Entity acquired = mc.level.getEntity(acquiredId);

        if(origin instanceof LivingEntity && acquired instanceof LivingEntity)
        {
            LivingEntity livingAcquired = (LivingEntity)acquired;
            me.ichun.mods.morph.client.entity.EntityAcquisition ent = Morph.EntityTypes.ACQUISITION.create(mc.level);
            if (ent != null) {
                ent.setTargets((LivingEntity)origin, livingAcquired, isMorphAcquisition);
                mc.level.addEntity(ent);
            }
            
            if(livingAcquired != mc.player)
            {
                livingAcquired.remove(Entity.RemovalReason.DISCARDED);
            }
            else
            {
                if (origin instanceof net.minecraft.world.entity.LivingEntity livingOrigin) {
                    livingAcquired.setYRot(livingOrigin.getYRot());
                    livingAcquired.setXRot(livingOrigin.getXRot());
                    livingAcquired.yHeadRot = livingOrigin.yHeadRot;
                    livingAcquired.yBodyRot = livingOrigin.yBodyRot;
                }
            }

            //block the hurt overlay/death rotation
            livingAcquired.setPos(acquired.getX(), acquired.getY(), acquired.getZ());
            livingAcquired.setYRot(acquired.getYRot());
            livingAcquired.setXRot(acquired.getXRot());
            
            livingAcquired.yBodyRotO = livingAcquired.yBodyRot;
            livingAcquired.oAttackAnim = livingAcquired.attackAnim;
            // limbSwingAmount update shifted to walkAnimation in 1.21
            livingAcquired.yHeadRotO = livingAcquired.yHeadRot;
            livingAcquired.yRotO = livingAcquired.getYRot();
            livingAcquired.xRotO = livingAcquired.getXRot();
            livingAcquired.deathTime = 0;
            livingAcquired.hurtTime = 0;
        }
    }
}
