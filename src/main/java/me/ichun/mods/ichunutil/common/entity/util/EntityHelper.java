package me.ichun.mods.ichunutil.common.entity.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EntityHelper
{
    public static Player getClientPlayer()
    {
        if(FMLEnvironment.dist.isClient())
        {
            return getMinecraftPlayer();
        }
        return null;
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static Player getMinecraftPlayer()
    {
        return Minecraft.getInstance().player;
    }

    public static float updateRotation(float oriRot, float intendedRot, float maxChange)
    {
        float var4 = Mth.wrapDegrees(intendedRot - oriRot);

        if(var4 > maxChange)
        {
            var4 = maxChange;
        }

        if(var4 < -maxChange)
        {
            var4 = -maxChange;
        }

        return oriRot + var4;
    }

    public static float sineifyProgress(float progress)
    {
        return (Mth.sin((float)Math.toRadians(-90F + (180F * progress))) / 2F) + 0.5F;
    }

    public static void faceEntity(Entity facer, Entity faced, float maxYaw, float maxPitch)
    {
        faceLocation(facer, faced.getX(), (faced instanceof LivingEntity ? faced.getEyeY() : (faced.getBoundingBox().minY + faced.getBoundingBox().maxY) / 2D), faced.getZ(), maxYaw, maxPitch);
    }

    public static void faceLocation(Entity facer, double posX, double posY, double posZ, float maxYaw, float maxPitch)
    {
        double d0 = posX - facer.getX();
        double d1 = posZ - facer.getZ();
        double d2 = posY - facer.getEyeY();

        double d3 = (double)Mth.sqrt((float) (d0 * d0 + d1 * d1));
        float f2 = (float)(Mth.atan2(d1, d0) * 180.0D / Math.PI) - 90.0F;
        float f3 = (float)(-(Mth.atan2(d2, d3) * 180.0D / Math.PI));
        facer.setXRot(updateRotation(facer.getXRot(), f3, maxPitch));
        facer.setYRot(updateRotation(facer.getYRot(), f2, maxYaw));
    }

    public static void playSound(@Nonnull Entity ent, SoundEvent soundEvent, SoundSource soundCategory, float volume, float pitch)
    {
        ent.level().playSound(ent.level().isClientSide ? getClientPlayer() : null, ent.getX(), ent.getEyeY(), ent.getZ(), soundEvent, soundCategory, volume, pitch);
    }

    public static CompoundTag getPlayerPersistentData(@Nonnull Player player, @Nullable String name)
    {
        // Persistent data in NeoForge 1.21.1 is handled via Custom Data Attachments if needed, 
        // but Forge's getPersistentData() generally maps to a CompoundTag on the player.
        CompoundTag persistedTag = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).orElseGet(CompoundTag::new);
        if(!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG))
        {
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedTag);
        }
        
        if(name == null)
        {
            return persistedTag;
        }
        CompoundTag specificTag = persistedTag.getCompound(name).orElseGet(CompoundTag::new);
        if(!persistedTag.contains(name))
        {
            persistedTag.put(name, specificTag);
        }
        return specificTag;
    }
}
