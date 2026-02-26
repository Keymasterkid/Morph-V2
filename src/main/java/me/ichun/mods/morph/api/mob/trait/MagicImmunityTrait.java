package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class MagicImmunityTrait extends Trait<MagicImmunityTrait>
        implements IEventBusRequired
{
    public transient float lastStrength = 0F;

    public MagicImmunityTrait()
    {
        type = "traitImmunityMagic";
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;
    }

    @Override
    public MagicImmunityTrait copy()
    {
        return new MagicImmunityTrait();
    }

    @SubscribeEvent
    public void onLivingAttack(LivingIncomingDamageEvent event)
    {
        if(lastStrength == 1F && event.getEntity() == player && (event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC) || event.getSource().is(DamageTypes.THORNS)))
        {
            event.setCanceled(true);
        }
    }
}
