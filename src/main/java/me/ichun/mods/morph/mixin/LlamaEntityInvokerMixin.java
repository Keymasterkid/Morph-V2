package me.ichun.mods.morph.mixin;

import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.minecraft.world.entity.animal.horse.Llama.class)
public interface LlamaEntityInvokerMixin
{
    @Invoker("setVariant")
    void callSetVariant(net.minecraft.world.entity.animal.horse.Llama.Variant variant);
}
