package me.ichun.mods.morph.client.entity;

import me.ichun.mods.ichunutil.client.tracker.ClientEntityTracker;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class EntityBiomassAbility extends Entity
{
    @Nonnull
    public Player player;

    public int fadeTime;
    public int solidTime;
    public int age;
    public MorphRenderHandler.ModelPartCapture capture = new MorphRenderHandler.ModelPartCapture();

    public EntityBiomassAbility(EntityType<? extends EntityBiomassAbility> entityTypeIn, Level levelIn)
    {
        this(entityTypeIn, levelIn, net.minecraft.world.entity.EntitySpawnReason.LOAD); // Default for compatibility
    }

    public EntityBiomassAbility(EntityType<? extends EntityBiomassAbility> entityTypeIn, Level levelIn, net.minecraft.world.entity.EntitySpawnReason spawnReason)
    {
        super(entityTypeIn, levelIn);
        setInvisible(true);
        setInvulnerable(true);
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        return false;
    }

    public EntityBiomassAbility setInfo(@Nonnull Player player, int fadeTime, int solidTime)
    {
        this.player = player;
        this.fadeTime = fadeTime;
        this.solidTime = solidTime;

        syncWithOriginPosition();

        return this;
    }

    @Override
    public void tick()
    {
        super.tick();

        age++;

        if(!player.isAlive() || !player.level().dimension().equals(this.level().dimension())) //parent is "dead"
        {
            if(player.isRemoved())
            {
                this.discard();
            }
        }
        else if(age > (fadeTime * 2) + solidTime)
        {
            this.discard();
        }
        else //parent is "alive" and safe
        {
            this.setPos(player.getX(), player.getY() + (player.getDimensions(net.minecraft.world.entity.Pose.STANDING).height() / 2D), player.getZ());
            this.setRot(player.getYRot(), player.getXRot());
        }
    }

    // @Override
    public AABB getRenderBoundingBox()
    {
        return player.getBoundingBox();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return player.shouldRenderAtSqrDistance(distance);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput compound)
    {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput compound)
    {
    }
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity)
    {
        // Stubbed for client-only entity
        return null;
    }

    public float getSkinAlpha(float partialTick)
    {
        float alpha;
        if(age < fadeTime)
        {
            alpha = EntityHelper.sineifyProgress(Mth.clamp((age + partialTick) / fadeTime, 0F, 1F));
        }
        else if(age >= fadeTime + solidTime)
        {
            alpha = EntityHelper.sineifyProgress(1F - Mth.clamp((age - (fadeTime + solidTime) + partialTick) / fadeTime, 0F, 1F));
        }
        else
        {
            alpha = 1F;
        }
        return alpha;
    }

    public void syncWithOriginPosition()
    {
        this.setPos(player.getX(), player.getY(), player.getZ());
        this.setRot(player.getYRot(), player.getXRot());
        this.xo = player.xo;
        this.yo = player.yo;
        this.zo = player.zo;

        this.xOld = player.xOld;
        this.yOld = player.yOld;
        this.zOld = player.zOld;
    }
}
