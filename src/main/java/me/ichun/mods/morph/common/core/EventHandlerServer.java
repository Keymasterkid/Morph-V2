package me.ichun.mods.morph.common.core;

import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.biomass.BiomassUpgradeHandler;
import me.ichun.mods.morph.common.command.CommandMorph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.MorphInfoImpl;
import me.ichun.mods.morph.common.morph.save.MorphSavedData;
import me.ichun.mods.morph.common.packet.PacketPlayerData;
import me.ichun.mods.morph.common.packet.PacketSessionSync;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
// AttachCapabilitiesEvent removed in 1.21 - use Data Attachments
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public class EventHandlerServer
{
    // Capabilities removed in NeoForge 1.21, replaced by Data Attachments. MorphInfo is now managed differently.

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event)
    {
        //We're trying to add a morph entity to the world, cancel this event
        if(event.getEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingAttacked(LivingIncomingDamageEvent event)
    {
        //The entity attacking is a morph. Cancel the event.
        if(event.getSource().getDirectEntity() != null && event.getSource().getDirectEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID))
        {
            event.setCanceled(true);
        }

        //The entity getting hurt is a morph. Cancel the event.
        if(event.getEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID)) // Do not cancel if it's from a kill command
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event)
    {
        //The entity dying is a morph. Cancel the event.
        if(event.getEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID))
        {
            event.setCanceled(true);
            return;
        }

        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof ServerPlayer source && !(source instanceof FakePlayer) && !source.isRemoved() && event.getEntity().getId() > 0)
        {
            MorphHandler.INSTANCE.handleMurderEvent((ServerPlayer)event.getSource().getEntity(), event.getEntity());
        }
    }

    @SubscribeEvent
    public void onEntityDimensions(net.neoforged.neoforge.event.entity.EntityEvent.Size event)
    {
        if(event.getEntity() instanceof Player && !event.getEntity().isRemoved() && event.getEntity().getId() > 0 && event.getEntity().tickCount >= 0)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)event.getEntity());
            if(info.isMorphed())
            {
                event.setNewSize(info.getMorphSize(1F));
            }
        }
    }

    // onPlayerTick was previously broken; player ticking is handled by MorphInfoImpl's tick() called from other events.

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if(!(event.getEntity().getServer().isSingleplayer() && event.getEntity().getGameProfile().getName().equals(event.getEntity().getServer().getSingleplayerProfile().getName()))) //if the player is not the client in singleplayer
        {
            Morph.channel.sendTo(new PacketSessionSync(BiomassUpgradeHandler.BIOMASS_UPGRADES.values()), (ServerPlayer)event.getEntity());
        }
        Morph.channel.sendTo(new PacketPlayerData(MorphHandler.INSTANCE.getPlayerMorphData(event.getEntity()).write(new CompoundTag())), (ServerPlayer)event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event)
    {
        MorphHandler.INSTANCE.getMorphInfo(event.getEntity()).read(MorphHandler.INSTANCE.getMorphInfo(event.getOriginal()).write(new CompoundTag()));
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event)
    {
        if(!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))
        {
            MorphHandler.INSTANCE.setSaveData(serverLevel.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(MorphSavedData::new, MorphSavedData::load, null), MorphSavedData.ID));
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        CommandMorph.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) //do this early so we do it before the server loads our Level save.
    {
        BiomassUpgradeHandler.loadBiomassUpgrades();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event)
    {
        MorphHandler.INSTANCE.setSaveData(null);
    }
}
