package me.ichun.mods.morph.api.mob.trait;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class ExplosiveImmunityTrait extends Trait<ExplosiveImmunityTrait>
        implements IEventBusRequired
{
    public transient float lastStrength = 0F;

    public ExplosiveImmunityTrait()
    {
        type = "traitImmunityExplosive";
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;
    }

    @Override
    public ExplosiveImmunityTrait copy()
    {
        return new ExplosiveImmunityTrait();
    }

    @SubscribeEvent
    public void onLivingAttack(LivingIncomingDamageEvent event)
    {
        if(lastStrength == 1F && event.getEntity() == player && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))
        {
            event.setCanceled(true);
        }
    }
}
