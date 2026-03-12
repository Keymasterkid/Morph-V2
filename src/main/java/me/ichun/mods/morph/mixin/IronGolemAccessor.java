package me.ichun.mods.morph.mixin;

import net.minecraft.world.entity.animal.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IronGolem.class)
public interface IronGolemAccessor {
    @Accessor("attackAnimationTick")
    void setAttackAnimationTick(int attackAnimationTick);
}
