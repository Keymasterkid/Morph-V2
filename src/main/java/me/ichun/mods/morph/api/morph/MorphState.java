package me.ichun.mods.morph.api.morph;

import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.mob.trait.Trait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.NonNullList;
import me.ichun.mods.morph.mixin.InventoryAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.IronGolem;
import me.ichun.mods.morph.mixin.IronGolemAccessor;
import me.ichun.mods.morph.mixin.WalkAnimationStateAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

public class MorphState implements Comparable<MorphState>
{
    public MorphVariant variant;
    private LivingEntity entInstance;
    private final Collection<ItemEntity> entInstanceDropCapture = new ArrayList<>();
    public float renderedShadowSize;
    public ArrayList<Trait<?>> traits = new ArrayList<>();

    private MorphState(){}

    public MorphState(MorphVariant variant, Player player)
    {
        this.variant = variant;
        this.traits = MorphApi.getApiImpl().getTraitsForVariant(variant, player);
    }

    //For Traits
    public void activateHooks()
    {
        for(Trait<?> trait : traits)
        {
            trait.addHooks();
        }
    }
    public void deactivateHooks()
    {
        for(Trait<?> trait : traits)
        {
            trait.removeHooks();
        }
    }

    public void tick(Player player, boolean resetInventory)
    {
        LivingEntity livingInstance = getEntityInstance(player.level(), player);
        livingInstance.captureDrops(entInstanceDropCapture);
        entInstanceDropCapture.clear();

        syncInventory(livingInstance, player, true);
        
        syncEntityWithPlayer(livingInstance, player);
        
        // Sync walk animation state before tick to ensure speedOld is correct
        WalkAnimationStateAccessor mobWalk = (WalkAnimationStateAccessor)livingInstance.walkAnimation;
        WalkAnimationStateAccessor playerWalk = (WalkAnimationStateAccessor)player.walkAnimation;
        mobWalk.setSpeedOld(playerWalk.getSpeedOld());
        mobWalk.setSpeed(playerWalk.getSpeed());
        mobWalk.setPosition(playerWalk.getPosition());

        livingInstance.tick();

        // Fix double-incrementing: ensure the position matches the player exactly after the tick.
        mobWalk.setPosition(playerWalk.getPosition());

        if(!resetInventory)
        {
            syncInventory(livingInstance, player, false);
        }
    }

    @Deprecated
    public static void syncEntityVisuals(LivingEntity living, Player player)
    {
        // deprecated by speed sync in tick()
    }

    public void tickTraits()
    {
        for(Trait<?> trait : traits)
        {
            trait.doTick(1F);
        }
    }

    @Nonnull
    @Deprecated
    public LivingEntity getEntityInstance(Level level, @Nullable UUID playerId)
    {
        return getEntityInstance(level, playerId != null ? level.getPlayerByUUID(playerId) : null);
    }

    @Nonnull
    public LivingEntity getEntityInstance(Level level, @Nullable Player player)
    {
        if(entInstance == null || entInstance.level() != level)
        {
            entInstance = variant.createEntityInstance(level, player);

            for(Trait<?> trait : traits)
            {
                trait.livingInstance = entInstance;
            }
        }

        return entInstance;
    }

    public CompoundTag write(CompoundTag tag)
    {
        tag.put("variant", variant.write(new CompoundTag()));
        return tag;
    }

    public void read(CompoundTag tag)
    {
        variant = MorphVariant.createFromNBT(tag.getCompound("variant").orElse(new net.minecraft.nbt.CompoundTag()));
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof MorphState)
        {
            MorphState state = (MorphState)obj;
            return Objects.equals(variant, state.variant);
        }
        return false;
    }

    @Override
    public int compareTo(MorphState o)
    {
        return variant.compareTo(o.variant);
    }

    public static MorphState createFromNbt(CompoundTag tag)
    {
        MorphState state = new MorphState();
        state.read(tag);
        return state;
    }

    public static void syncEntityPosRotWithPlayer(LivingEntity living, Player player)
    {
        living.tickCount = player.tickCount;

        living.setPos(player.getX(), player.getY(), player.getZ());
        living.setYRot(player.getYRot());
        living.setXRot(player.getXRot());
        living.xo = player.xo;
        living.yo = player.yo;
        living.zo = player.zo;

        living.xOld = player.xOld;
        living.yOld = player.yOld;
        living.zOld = player.zOld;

        living.yRotO = player.yRotO;
        living.xRotO = player.xRotO;

        living.yHeadRot = player.yHeadRot;
        living.yHeadRotO = player.yHeadRotO;

        living.yBodyRot = player.yBodyRot;
        living.yBodyRotO = player.yBodyRotO;

        living.getActiveEffectsMap().clear();
    }

    public static void syncEntityWithPlayer(LivingEntity living, Player player)
    {
        syncEntityPosRotWithPlayer(living, player);

        living.setDeltaMovement(player.getDeltaMovement());

        living.horizontalCollision = player.horizontalCollision;
        living.verticalCollision = player.verticalCollision;
        living.setOnGround(player.onGround());
        living.setShiftKeyDown(player.isShiftKeyDown());
        living.setSwimming(player.isSwimming());
        living.setSprinting(player.isSprinting());

        living.setHealth(living.getMaxHealth() * (player.getHealth() / player.getMaxHealth()));
        living.hurtTime = player.hurtTime;
        living.deathTime = player.deathTime;

        living.swingTime = player.swingTime;
        living.swinging = player.swinging;
        living.swingingArm = player.swingingArm;
        living.attackAnim = player.attackAnim;
        living.oAttackAnim = player.oAttackAnim;

        Pose pose = living.getPose();
        living.setPose(player.getPose());

        if(pose != living.getPose())
        {
            living.refreshDimensions();
        }

        if(player.getSleepingPos().isPresent())
        {
            living.setSleepingPos(player.getSleepingPos().get());
        }
        else
        {
            living.stopSleeping();
        }

        living.setInvisible(player.isInvisible());

        living.setGlowingTag(player.hasGlowingTag());

        living.setRemainingFireTicks(player.getRemainingFireTicks());

        living.getActiveEffectsMap().putAll(player.getActiveEffectsMap());

        if (living instanceof IronGolem golem) {
            if (player.swingTime > 0 && player.swingTime < 10) {
                ((IronGolemAccessor)golem).setAttackAnimationTick(10 - player.swingTime);
            } else if (player.swingTime == 0) {
                ((IronGolemAccessor)golem).setAttackAnimationTick(0);
            }
        }

        specialEntityPlayerSync(living, player);
    }

    public static void specialEntityPlayerSync(LivingEntity living, Player player)
    {
        for(BiConsumer<LivingEntity, Player> consumer : MorphApi.getApiImpl().getModPlayerMorphSyncConsumers())
        {
            consumer.accept(living, player);
        }
    }

    public static void syncInventory(LivingEntity living, Player player, boolean reset)
    {
        if(living instanceof Player)
        {
            Player playerEnt = (Player)living;

            InventoryAccessor inv = (InventoryAccessor)playerEnt.getInventory();
            for(EquipmentSlot value : EquipmentSlot.values())
            {
                boolean shouldReset = reset && (value == EquipmentSlot.MAINHAND || value == EquipmentSlot.OFFHAND);
                    ItemStack copy = shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value).copy();
                    if (value == EquipmentSlot.MAINHAND) {
                        inv.getItems().set(inv.getSelected(), copy);
                    } else {
                        // In 1.21.8, armor and offhand are managed by EntityEquipment
                        inv.getEquipment().set(value, copy);
                    }
            }
        }
        else
        {
            for(EquipmentSlot value : EquipmentSlot.values())
            {
                boolean shouldReset = reset && (value == EquipmentSlot.MAINHAND || value == EquipmentSlot.OFFHAND);
                if(!ItemStack.isSameItemSameComponents(living.getItemBySlot(value), shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value)))
                {
                    living.setItemSlot(value, shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value).copy());
                }
            }
        }

        if(player.isUsingItem())
        {
            if(player.getUseItemRemainingTicks() == 1)
            {
                InteractionHand InteractionHand = player.getUsedItemHand();
                living.startUsingItem(InteractionHand);
            }
        }
        else
        {
            living.stopUsingItem();
        }
    }
}
