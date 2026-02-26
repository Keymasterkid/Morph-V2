package me.ichun.mods.ichunutil.common.item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
public interface DualHandedItem {
    default boolean isHeldLikeBow(ItemStack is, LivingEntity living) { return false; }
    default boolean isHeldLikeBow(ItemStack is, Player player) { return false; }
    static boolean isItemDualHanded(ItemStack is) { return false; }
    static boolean canItemBeUsed(Player player, ItemStack is) { return false; }
}
