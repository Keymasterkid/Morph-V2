package me.ichun.mods.ichunutil.client.tracker.entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
public class EntityTracker extends Entity {
    public EntityTracker(EntityType<?> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {}
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {}
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
}
