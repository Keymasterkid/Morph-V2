package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class DamageSourceImmunityTrait extends Trait<DamageSourceImmunityTrait>
        implements IEventBusRequired
{
    public String damageType;

    public transient float lastStrength = 0F;

    public DamageSourceImmunityTrait()
    {
        type = "traitImmunityDamageSource";
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;
    }

    @Override
    public DamageSourceImmunityTrait copy()
    {
        DamageSourceImmunityTrait ds = new DamageSourceImmunityTrait();
        ds.damageType = this.damageType;
        return ds;
    }

    @Override
    public boolean canTransitionTo(Trait<?> trait)
    {
        if(trait instanceof DamageSourceImmunityTrait)
        {
            return damageType != null && damageType.equals(((DamageSourceImmunityTrait)trait).damageType);
        }
        return false;
    }

    @SubscribeEvent
    public void onLivingAttack(LivingIncomingDamageEvent event)
    {
        if(lastStrength == 1F && event.getEntity() == player && event.getSource().is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(damageType))))
        {
            event.setCanceled(true);
        }
    }
}
