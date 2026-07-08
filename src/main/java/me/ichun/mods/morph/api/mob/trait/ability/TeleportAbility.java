package me.ichun.mods.morph.api.mob.trait.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;

public class TeleportAbility extends Ability<TeleportAbility>
{
    public Double distance;

    public TeleportAbility()
    {
        type = "abilityTeleport";
    }

    @Override
    public void addHooks()
    {
        if(distance == null)
        {
            distance = 32.0;
        }
        super.addHooks();
    }

    @Override
    public void tick(float strength)
    {
    }

    @Override
    public void onAction(boolean isDown)
    {
        if(isDown && player instanceof ServerPlayer && !player.getCooldowns().isOnCooldown(net.minecraft.world.item.Items.ENDER_PEARL))
        {
            Level level = player.level();
            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 lookVec = player.getViewVector(1.0F);
            Vec3 targetVec = eyePos.add(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance);

            ClipContext context = new ClipContext(eyePos, targetVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
            HitResult hitResult = level.clip(context);

            Vec3 hitPos = hitResult.getLocation();
            
            double targetX = hitPos.x;
            double targetY = hitPos.y;
            double targetZ = hitPos.z;

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
                targetX = pos.getX() + 0.5;
                targetY = pos.getY();
                targetZ = pos.getZ() + 0.5;
            }

            boolean success = player.randomTeleport(targetX, targetY, targetZ, true);
            
            if(success)
            {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.getCooldowns().addCooldown(net.minecraft.world.item.Items.ENDER_PEARL, 40);
            }
        }
    }

    @Override
    public TeleportAbility copy()
    {
        TeleportAbility ability = new TeleportAbility();
        ability.distance = this.distance;
        return ability;
    }

    @Override
    public boolean isActive()
    {
        return true;
    }
}
