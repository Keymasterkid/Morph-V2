package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.minecraft.resources.ResourceLocation;
import java.util.Random;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
public class SwimmerTrait extends Trait<SwimmerTrait>
        implements IEventBusRequired
{
    public Float swimMultiplier;
    public Float landMultiplier;
    public Boolean doNotAffectFog;

    public transient float lastStrength = 0F;
    public transient Random rand = new Random();
    public transient float lastSwimMul = 1F;
    public transient boolean doNotRemoveAttribute;

    public SwimmerTrait()
    {
        type = "traitSwimmer";
    }

    @Override
    public void addHooks()
    {
        if(!(doNotAffectFog != null && doNotAffectFog))
        {
            super.addHooks();
        }

        if(swimMultiplier == null)
        {
            swimMultiplier = 1F;
        }

        if(landMultiplier == null)
        {
            landMultiplier = 1F;
        }
    }

    @Override
    public void removeHooks()
    {
        super.removeHooks();

        if(!doNotRemoveAttribute)
        {
            setSwimAttribute(1F);
        }
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;

        if(swimMultiplier != 0F)
        {
            if(player.isInWater())
            {
                setSwimAttribute(1F + ((swimMultiplier - 1F) * strength));
            }
        }

        if(landMultiplier != 0F)
        {
            if(!player.isInWater() && player.onGround())
            {
                multiplyMotion(1F + ((landMultiplier - 1F) * strength));
            }
        }
    }

    @Override
    public void transitionalTick(SwimmerTrait prevTrait, float transitionProgress)
    {
        prevTrait.doNotRemoveAttribute = true;

        float swimMul = Mth.lerp(transitionProgress, prevTrait.swimMultiplier, swimMultiplier);
        if(swimMul != 0F)
        {
            if(player.isInWater())
            {
                setSwimAttribute(swimMul);
            }
        }

        float landMul = Mth.lerp(transitionProgress, prevTrait.landMultiplier, landMultiplier);
        if(landMul != 0F)
        {
            if(!player.isInWater() && player.onGround())
            {
                multiplyMotion(landMul);
            }
        }
    }

    public void setSwimAttribute(float mul)
    {
        if(player.level().isClientSide)
        {
            return;
        }

        final AttributeInstance playerAttribute = player.getAttribute(NeoForgeMod.SWIM_SPEED);
        if(playerAttribute != null)
        {
            if(lastSwimMul != mul)
            {
                lastSwimMul = mul;

                rand.setSeed(Math.abs("MorphAttr".hashCode() * 1231543 + "traitSwimmer".hashCode() * 268));
                UUID uuid = UUID.nameUUIDFromBytes("MorphAttributeModifier:traitSwimmer".getBytes());

                //you can't reapply the same modifier, so lets remove it
                playerAttribute.removeModifier(ResourceLocation.fromNamespaceAndPath("morph", "trait_swimmer"));

                if(mul != 1F)
                {
                    playerAttribute.addTransientModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath("morph", "trait_swimmer"), (double)mul - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            }
        }
    }

    public void multiplyMotion(float mul)
    {
        player.setDeltaMovement(player.getDeltaMovement().multiply(mul, mul, mul));
    }

    @Override
    public boolean canTransitionTo(Trait<?> trait)
    {
        if(trait instanceof SwimmerTrait)
        {
            return doNotAffectFog == ((SwimmerTrait)trait).doNotAffectFog;
        }
        return false;
    }

    @Override
    public SwimmerTrait copy()
    {
        SwimmerTrait trait = new SwimmerTrait();
        trait.swimMultiplier = this.swimMultiplier;
        trait.landMultiplier = this.landMultiplier;
        trait.doNotAffectFog = this.doNotAffectFog;
        return trait;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onFogDensity(ViewportEvent.RenderFog event)
    {
        if(event.getCamera().getEntity() == player)
        {
            boolean inWater = player.isEyeInFluid(FluidTags.WATER);
            boolean inLava = player.isEyeInFluid(FluidTags.LAVA);
            Entity entity = event.getCamera().getEntity();

            //Taken from FogRenderer.setupFog

            //If the camera view is in water
            if (inWater)
            {
                float fogDensity = 0.05F;

                if (entity instanceof LocalPlayer) {
                    LocalPlayer LocalPlayer = (LocalPlayer)entity;
                    fogDensity -= LocalPlayer.getWaterVision() * LocalPlayer.getWaterVision() * 0.03F;
                    if (LocalPlayer.level().getBiome(LocalPlayer.blockPosition()).is(Biomes.SWAMP) || LocalPlayer.level().getBiome(LocalPlayer.blockPosition()).is(Biomes.MANGROVE_SWAMP)) {
                        fogDensity += 0.005F;
                    }
                }

                fogDensity *= 1F + (-0.5F * lastStrength);

                event.setFarPlaneDistance(1F / fogDensity); // Crude conversion
                event.setNearPlaneDistance(0F);
                // RenderSystem.fogMode(GlStateManager.FogMode.EXP2); // EXP/EXP2 is less direct now
            }
            else if(landMultiplier < 1F && !inLava) //if there is a <1 land multiplier and you are on land
            {
                float farPlaneDistance = Math.max(event.getFarPlaneDistance() - 16.0F, 32.0F);
                float f1 = Mth.lerp(lastStrength, farPlaneDistance, 5.0F);
                float f2 = f1 * 0.25F;
                float f3 = f1;

                event.setFarPlaneDistance(f3);
                event.setNearPlaneDistance(f2);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onFogColor(ViewportEvent.ComputeFogColor event)
    {
        if(event.getCamera().getEntity() == player)
        {
            if (player.isEyeInFluid(FluidTags.WATER))
            {
                float multi = 1F + 4F * lastStrength;
                event.setRed(event.getRed() * multi);
                event.setBlue(event.getBlue() * multi);
                event.setGreen(event.getGreen() * multi);
            }
        }
    }
}
