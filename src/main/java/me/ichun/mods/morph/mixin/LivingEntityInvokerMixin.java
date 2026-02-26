package me.ichun.mods.morph.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvokerMixin
{
    @Invoker("getHurtSound")
    SoundEvent callGetHurtSound(DamageSource source);

    @Invoker("getDeathSound")
    SoundEvent callGetDeathSound();

    @Invoker("getDrinkingSound")
    SoundEvent callGetDrinkingSound(ItemStack stack);

    @Invoker("getSoundVolume")
    float callGetSoundVolume();

    @Invoker("getVoicePitch")
    float callGetVoicePitch();
}
