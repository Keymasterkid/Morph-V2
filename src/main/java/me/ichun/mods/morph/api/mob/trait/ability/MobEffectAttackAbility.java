package me.ichun.mods.morph.api.mob.trait.ability;

import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.mob.trait.IEventBusRequired;
import me.ichun.mods.morph.api.mob.trait.Trait;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class MobEffectAttackAbility extends Ability<MobEffectAttackAbility>
        implements IEventBusRequired
{
    public String MobEffectId;
    public Integer duration;
    public Integer amplifier;

    public transient MobEffect MobEffectObj;
    public transient float lastStrength = 0F;

    public MobEffectAttackAbility()
    {
        type = "abilityMobEffectAttack";
    }

    @Override
    public void addHooks()
    {
        if(MobEffectId != null)
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

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;
    }

    @Override
    public MobEffectAttackAbility copy()
    {
        MobEffectAttackAbility trait = new MobEffectAttackAbility();
        trait.MobEffectId = this.MobEffectId;
        return trait;
    }

    @Override
    public boolean canTransitionTo(Trait<?> trait)
    {
        if(trait instanceof MobEffectAttackAbility)
        {
            return MobEffectId != null && MobEffectId.equals(((MobEffectAttackAbility)trait).MobEffectId);
        }
        return false;
    }

    @SubscribeEvent
    public void onLivingAttack(LivingIncomingDamageEvent event)
    {
        if(lastStrength == 1F && event.getSource().getDirectEntity() == player && MorphApi.getApiImpl().canUseAbility(player, this))
        {
            event.getEntity().addEffect(new MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffectObj), duration != null ? duration : 200, amplifier != null ? amplifier: 0));
        }
    }
}
