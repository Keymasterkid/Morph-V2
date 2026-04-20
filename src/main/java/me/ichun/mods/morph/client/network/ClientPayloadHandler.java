package me.ichun.mods.morph.client.network;

import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import me.ichun.mods.morph.common.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handlePacketAcquisition(PacketAcquisition msg, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        Entity origin = mc.level.getEntity(msg.originId);
        Entity acquired = mc.level.getEntity(msg.acquiredId);

        if(origin instanceof LivingEntity && acquired instanceof LivingEntity)
        {
            LivingEntity livingAcquired = (LivingEntity)acquired;
            me.ichun.mods.morph.client.entity.EntityAcquisition ent = Morph.EntityTypes.ACQUISITION.create(mc.level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            if (ent != null) {
                ent.setTargets((LivingEntity)origin, livingAcquired, msg.isMorphAcquisition);
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

    public static void handlePacketInvalidateClientHealth(PacketInvalidateClientHealth msg, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Trigger a dimension refresh which often forces a HUD/data update.
            mc.player.refreshDimensions();
        }
    }

    public static void handlePacketUpdateMorph(PacketUpdateMorph msg, IPayloadContext context) {
        MorphVariant variant = MorphVariant.createFromNBT(msg.nbt);
        Morph.eventHandlerClient.updateMorph(variant);
    }

    public static void handlePacketUpdateBiomassValue(PacketUpdateBiomassValue msg, IPayloadContext context) {
        Morph.eventHandlerClient.morphData.biomass = msg.value;
        Morph.eventHandlerClient.hudHandler.updateBiomass(Morph.eventHandlerClient.morphData);
    }

    public static void handlePacketUpdateBiomassUpgrades(PacketUpdateBiomassUpgrades msg, IPayloadContext context) {
        Morph.eventHandlerClient.morphData.upgrades = msg.upgrades;
        Morph.eventHandlerClient.hudHandler.updateBiomass(Morph.eventHandlerClient.morphData);
    }

    public static void handlePacketPlayerData(PacketPlayerData msg, IPayloadContext context) {
        PlayerMorphData playerMorphData = new PlayerMorphData();
        playerMorphData.read(msg.nbt);
        Morph.eventHandlerClient.setPlayerMorphData(playerMorphData);
    }

    public static void handlePacketOpenGenerator(PacketOpenGenerator msg, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if(msg.targetId >= 0)
        {
            Entity target = mc.level.getEntity(msg.targetId);

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

    public static void handlePacketMorphInfo(PacketMorphInfo msg, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(msg.entId);
        if(entity instanceof Player && !entity.isRemoved())
        {
            MorphHandler.INSTANCE.getMorphInfo((Player)entity).read(msg.nbt);
        }
    }

    public static void registerConfigScreen(net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
            (mc, parent) -> new me.ichun.mods.ichunutil.client.gui.config.WorkspaceConfigs(parent));
    }
}
