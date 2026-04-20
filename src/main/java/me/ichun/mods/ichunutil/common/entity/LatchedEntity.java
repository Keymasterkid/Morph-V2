package me.ichun.mods.ichunutil.common.entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
public class LatchedEntity<M extends Entity> extends Entity {
    public LatchedEntity(EntityType<?> t, Level l) { this(t, l, EntitySpawnReason.LOAD); }
    public LatchedEntity(EntityType<?> t, Level l, EntitySpawnReason r) { super(t, l); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {}
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {}
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
}
