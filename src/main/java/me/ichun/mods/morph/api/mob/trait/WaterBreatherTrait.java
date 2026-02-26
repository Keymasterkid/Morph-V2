package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;

public class WaterBreatherTrait extends Trait<WaterBreatherTrait>
        implements IEventBusRequired
{
    public Boolean suffocatesOnLand;

    public transient int air = -100;

    public WaterBreatherTrait()
    {
        type = "traitWaterBreather";
    }

    @Override
    public void tick(float strength)
    {
        if(strength == 1F && player.isAlive())
        {
            if(air == -100)
            {
                air = player.getAirSupply();
            }

            if(player.isInWater() || player.level().getBlockState(player.blockPosition()).getBlock() == net.minecraft.world.level.block.Blocks.WATER)
            {
                air = Math.min(air + 4, player.getMaxAirSupply());
                player.setAirSupply(air);
            }
            else if (suffocatesOnLand != null && suffocatesOnLand) //if the player is on land and the entity suffocates
            {
                //taken from decreaseAirSupply in Living Entity
                int i = (int)player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.OXYGEN_BONUS);
                air = i > 0 && player.getRandom().nextInt(i + 1) > 0 ? air : air - 1;

                if (air == -20) {
                    air = 0;

                    for(int j = 0; j < 8; ++j) {
                        float f = player.getRandom().nextFloat() - player.getRandom().nextFloat();
                        float f1 = player.getRandom().nextFloat() - player.getRandom().nextFloat();
                        float f2 = player.getRandom().nextFloat() - player.getRandom().nextFloat();
                        player.level().addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE, player.getX() + (double)f, player.getY() + (double)f1, player.getZ() + (double)f2, player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z);
                    }

                    player.hurt(player.level().damageSources().drown(), 2.0F);
                }

                player.setAirSupply(air);
            }
        }
    }

    @Override
    public WaterBreatherTrait copy()
    {
        WaterBreatherTrait trait = new WaterBreatherTrait();
        trait.suffocatesOnLand = this.suffocatesOnLand;
        return trait;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRenderGameOverlayPre(RenderGuiLayerEvent.Pre event)
    {
        if(event.getName().equals(VanillaGuiLayers.AIR_LEVEL) && net.minecraft.client.Minecraft.getInstance().cameraEntity == player)
        {
            //No need to draw the air bubbles if air < 300, default GUI already does that.
            if(player.isEyeInFluid(FluidTags.WATER) && air >= 300) //player's in water but also max air.
            {
                event.setCanceled(true);
            }
        }
    }
}
