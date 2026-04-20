package me.ichun.mods.morph.mixin;

import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin
{

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void morph$getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {

        if (!((Object) this instanceof Player player)) return;

        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(player);
        if (info != null && info.isMorphed()) {
            cir.setReturnValue(info.getActiveMorphSizeByPose(pose));
        }
    }



    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    protected void playStepSound(BlockPos pos, BlockState blockState, CallbackInfo ci)
    {
        if(((Entity)(Object)this) instanceof Player)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
            if(info != null && info.isMorphed())
            {
                info.playStepSound(pos, blockState);
                ci.cancel();
            }
        }
    }

    @Inject(method = "playSwimSound", at = @At("HEAD"), cancellable = true)
    protected void playSwimSound(float volume, CallbackInfo ci)
    {
        if(((Entity)(Object)this) instanceof Player)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
            if(info != null && info.isMorphed())
            {
                info.playSwimSound(volume);
                ci.cancel();
            }
        }
    }
}
