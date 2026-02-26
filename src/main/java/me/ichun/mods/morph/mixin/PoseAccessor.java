package me.ichun.mods.morph.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PoseStack.Pose.class)
public interface PoseAccessor
{
    @Invoker("<init>")
    static PoseStack.Pose create(Matrix4f pose, Matrix3f normal)
    {
        throw new UnsupportedOperationException();
    }
}
