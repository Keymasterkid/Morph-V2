package me.ichun.mods.morph.client.core;

import me.ichun.mods.ichunutil.client.key.KeyBind;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.MorphInfoImpl;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import me.ichun.mods.morph.common.packet.PacketRequestMorphInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;

public class EventHandlerClient
{
    public PlayerMorphData morphData;
    public HudHandler hudHandler;

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event)
    {
        if(MorphRenderHandler.isRenderingMorph) //we're rendering a player morph, forgetaboutit
        {
            return;
        }

        Player player = event.getEntity();

        //Disables the render of this player if this player is riding the render view entity and the game is in first person
        if(Morph.configClient.morphDisableRidingPlayerRenderInFirstPerson && player.getVehicle() == net.minecraft.client.Minecraft.getInstance().cameraEntity && net.minecraft.client.Minecraft.getInstance().options.getCameraType().equals(CameraType.FIRST_PERSON))
        {
            event.setCanceled(true);
            return;
        }

        MorphRenderHandler.restoreShadowSize(event.getRenderer());

        if(!player.isRemoved())
        {
            MorphInfoImpl info = (MorphInfoImpl)MorphHandler.INSTANCE.getMorphInfo(player);
            if(!info.requested)
            {
                Morph.channel.sendToServer(new PacketRequestMorphInfo(player.getGameProfile().getId()));
                info.requested = true;
            }
            else if(info.isMorphed())
            {
                event.setCanceled(true);

                MorphRenderHandler.renderMorphInfo(player, info, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());

                MorphRenderHandler.setShadowSize(event.getRenderer(), info, event.getPartialTick());
            }
        }
    }

    @SubscribeEvent
    public void onRenderNameplate(RenderNameTagEvent event)
    {
        if(MorphRenderHandler.denyRenderNameplate || event.getEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID) && !MorphRenderHandler.isRenderingMorph)
        {
            // event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderTick(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event)
    {
        if(true /* handled by Pre event class */ && net.minecraft.client.Minecraft.getInstance().player != null && !net.minecraft.client.Minecraft.getInstance().player.isRemoved())
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(net.minecraft.client.Minecraft.getInstance().player);
            if(info.isMorphed() && (info.getMorphProgress(/* render tick time */ 0.0f) < 1F || Morph.configServer.aggressiveSizeRecalculation)) //is morphing
            {
                // net.minecraft.client.Minecraft.getInstance().player.eyeHeight = info.getMorphEyeHeight(/* render tick time */ 0.0f);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event)
    {
        if(!event.getEntity().isRemoved() && event.getEntity().level().isClientSide() && event.getEntity() == net.minecraft.client.Minecraft.getInstance().player && event.getEntity().tickCount == 10)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(event.getEntity());
            if(!info.requested)
            {
                Morph.channel.sendToServer(new PacketRequestMorphInfo(event.getEntity().getGameProfile().getId()));
                info.requested = true;
            }
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event)
    {
        setPlayerMorphData(null);
    }

    public void handleInput(KeyBind keyBind, boolean isReleased)
    {
        if (hudHandler != null) {
            hudHandler.handleInput(keyBind, isReleased);
        }
    }

    public void updateMorph(MorphVariant variant)
    {
        if(morphData != null)
        {
            boolean handled = false;

            ArrayList<MorphVariant> morphs = morphData.morphs;
            for(int i = 0; i < morphs.size(); i++)
            {
                MorphVariant morph = morphs.get(i);
                if(morph.id.equals(variant.id))
                {
                    morphs.remove(i);
                    if(variant.hasVariants())
                    {
                        morphs.add(i, variant);
                    }
                    handled = true;
                    break;
                }
            }

            if(!handled && variant.hasVariants()) //presume a new morph
            {
                morphs.add(variant);
            }

            hudHandler.updateMorphs();

            Collections.sort(morphData.morphs); //sort in order of name.
        }
        else
        {
            Morph.LOGGER.error("We got morph data but we don't have the save data! Variant: {}", variant.id);
        }
    }

    public void setPlayerMorphData(PlayerMorphData playerMorphData)
    {
        morphData = playerMorphData;

        if(morphData == null) //disconnected, do cleanup
        {
            if(hudHandler != null)
            {
                hudHandler.destroy();

                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(hudHandler);
                hudHandler = null;
            }
        }
        else
        {
            morphData.morphs.removeIf(morph -> !morph.hasVariants()); //we remove those that don't have variants, don't care no more

            Collections.sort(morphData.morphs); //sort in order of name.

            if(hudHandler == null)
            {
                hudHandler = new HudHandler(net.minecraft.client.Minecraft.getInstance(), morphData);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(hudHandler);
            }
            else
            {
                hudHandler.updateBiomass(morphData);
                hudHandler.updateMorphs();
            }
        }
    }
}
