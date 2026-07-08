package me.ichun.mods.morph.api.mob.trait.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

public class FireballAbility extends Ability<FireballAbility>
{
    public Boolean isLarge;
    public Integer cooldown;

    public FireballAbility()
    {
        type = "abilityFireball";
    }

    @Override
    public void addHooks()
    {
        if(isLarge == null)
        {
            isLarge = false;
        }
        if(cooldown == null)
        {
            cooldown = 40;
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
        if(isDown && player instanceof ServerPlayer && !player.getCooldowns().isOnCooldown(net.minecraft.world.item.Items.FIRE_CHARGE))
        {
            Vec3 look = player.getViewVector(1.0F);
            double x = player.getX() + look.x * 1.5;
            double y = player.getEyeY() - 0.1;
            double z = player.getZ() + look.z * 1.5;

            if (isLarge) {
                LargeFireball fireball = new LargeFireball(player.level(), player, look.normalize(), 1);
                fireball.setPos(x, y, z);
                player.level().addFreshEntity(fireball);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                SmallFireball fireball = new SmallFireball(player.level(), player, look.normalize());
                fireball.setPos(x, y, z);
                player.level().addFreshEntity(fireball);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            player.getCooldowns().addCooldown(net.minecraft.world.item.Items.FIRE_CHARGE, cooldown);
        }
    }

    @Override
    public FireballAbility copy()
    {
        FireballAbility ability = new FireballAbility();
        ability.isLarge = this.isLarge;
        ability.cooldown = this.cooldown;
        return ability;
    }

    @Override
    public boolean isActive()
    {
        return true;
    }
}
