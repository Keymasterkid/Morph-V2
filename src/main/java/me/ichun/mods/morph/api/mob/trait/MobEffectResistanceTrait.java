package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class MobEffectResistanceTrait extends Trait<MobEffectResistanceTrait>
        implements IEventBusRequired
{
    @com.google.gson.annotations.SerializedName("effectId")
    public String MobEffectId;

    public transient MobEffect MobEffectObj;
    public transient float lastStrength = 0F;

    public MobEffectResistanceTrait()
    {
        type = "traitMobEffectResistance";
    }

    @Override
    public void addHooks()
    {
        if(MobEffectId != null)
        {
            if(MobEffectId.equals("*")) //immune to all MobEffects
            {
                super.addHooks();
            }
            else
            {
                ResourceLocation MobEffectRL = ResourceLocation.parse(MobEffectId);
                MobEffect theMobEffect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(MobEffectRL);
                if(theMobEffect != null)
                {
                    MobEffectObj = theMobEffect;
                    super.addHooks();
                }
            }
        }
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;

        if(MobEffectObj != null && lastStrength == 1F)
        {
            if(MobEffectId.equals("*")) //immune to all MobEffects
            {
                for(MobEffectInstance MobEffect : player.getActiveEffects())
                {
                    player.removeEffect(MobEffect.getEffect());
                }
            }
            else
            {
                MobEffectInstance potion = player.getEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffectObj));
                if(potion != null)
                {
                    player.removeEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffectObj));
                }
            }
        }
    }

    @Override
    public MobEffectResistanceTrait copy()
    {
        MobEffectResistanceTrait trait = new MobEffectResistanceTrait();
        trait.MobEffectId = this.MobEffectId;
        return trait;
    }

    @Override
    public boolean canTransitionTo(Trait<?> trait)
    {
        if(trait instanceof MobEffectResistanceTrait)
        {
            return MobEffectId != null && MobEffectId.equals(((MobEffectResistanceTrait)trait).MobEffectId);
        }
        return false;
    }

    @SubscribeEvent
    public void onPotionApplicable(MobEffectEvent.Applicable event)
    {
        if(lastStrength == 1F && event.getEntity() == player && (MobEffectId != null && MobEffectId.equals("*") || event.getEffectInstance().getEffect().value() == MobEffectObj))
        {
            event.setResult(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
