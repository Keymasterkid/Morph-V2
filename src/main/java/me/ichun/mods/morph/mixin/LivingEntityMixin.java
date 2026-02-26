package me.ichun.mods.morph.mixin;

import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
    @Inject(method = "getSoundVolume", at = @At("HEAD"), cancellable = true)
    protected void getSoundVolume(CallbackInfoReturnable<Float> cir)
    {
        if(((LivingEntity)(Object)this) instanceof Player)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
            if(info != null && info.isMorphed())
            {
                cir.setReturnValue(info.getSoundVolume());
            }
        }
    }

    @Inject(method = "getVoicePitch", at = @At("HEAD"), cancellable = true)
    protected void getVoicePitch(CallbackInfoReturnable<Float> cir)
    {
        if(((LivingEntity)(Object)this) instanceof Player)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
            if(info != null && info.isMorphed())
            {
                cir.setReturnValue(info.getSoundPitch());
            }
        }
    }

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void completeUsingItem(CallbackInfo ci)
    {
        if(((LivingEntity)(Object)this) instanceof Player)
        {
            Player player = (Player)(Object)this;
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(player);
            if(info != null && info.isMorphed())
            {
                ItemStack stack = player.getUseItem();
                if (!stack.isEmpty()) {
                    // Logic for 1.21.1 might need adjustment depending on how sounds are handled.
                }
            }
        }
    }

    @Inject(method = "getDrinkingSound", at = @At("HEAD"), cancellable = true)
    protected void getDrinkingSound(ItemStack stack, CallbackInfoReturnable<SoundEvent> cir)
    {
        if(((LivingEntity)(Object)this) instanceof Player)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
            if(info != null && info.isMorphed())
            {
                cir.setReturnValue(info.getDrinkSound(stack));
            }
        }
    }

    @Inject(method = "getEatingSound", at = @At("HEAD"), cancellable = true)
    protected void getEatingSound(ItemStack stack, CallbackInfoReturnable<SoundEvent> cir)
    {
        if(((LivingEntity)(Object)this) instanceof Player)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
            if(info != null && info.isMorphed())
            {
                cir.setReturnValue(info.getEatSound(stack));
            }
        }
    }
}
