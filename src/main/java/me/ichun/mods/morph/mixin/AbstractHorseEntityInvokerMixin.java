package me.ichun.mods.morph.mixin;

import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractHorse.class)
public interface AbstractHorseEntityInvokerMixin
{
    @Accessor("inventory")
    net.minecraft.world.SimpleContainer getInventory();

    @Invoker("setFlag")
    void callSetFlag(int id, boolean flag);
}
