package me.ichun.mods.morph.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.client.tracker.ClientEntityTracker;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.CameraType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
// NetworkHooks removed - use IPayload system

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
public class EntityAcquisition extends Entity
{
    @Nonnull
    public LivingEntity livingOrigin;
    @Nonnull
    public LivingEntity livingAcquired;

    public boolean isMorphAcquisition;
    public boolean hasCaptured = false;

    public ArrayList<Tendril> tendrils = new ArrayList<>();

    public int maxRequiredTendrils;
    public int age;

    public MorphRenderHandler.ModelPartCapture acquiredCapture = new MorphRenderHandler.ModelPartCapture();

    public EntityAcquisition(EntityType<? extends EntityAcquisition> entityTypeIn, Level levelIn, net.minecraft.world.entity.EntitySpawnReason spawnReason)
    {
        super(entityTypeIn, levelIn);
        setInvisible(true);
        setInvulnerable(true);
        this.setId(ClientEntityTracker.getNextEntId());
    }

    public EntityAcquisition(EntityType<? extends EntityAcquisition> entityTypeIn, Level levelIn)
    {
        this(entityTypeIn, levelIn, net.minecraft.world.entity.EntitySpawnReason.LOAD);
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        return false;
    }

    public EntityAcquisition setTargets(@Nonnull LivingEntity origin, @Nonnull LivingEntity acquired, boolean isMorphAcquisition)
    {
        this.livingOrigin = origin;
        this.livingAcquired = acquired;
        this.isMorphAcquisition = isMorphAcquisition;

        syncWithOriginPosition();

        if(isMorphAcquisition)
        {
            tendrils.add(new Tendril(null).headTowards(getTargetPos(), false));
        }
        return this;
    }

    @Override
    public void tick()
    {
        super.tick();

        age++;

        if(!livingOrigin.isAlive() || !livingOrigin.level().dimension().equals(this.level().dimension())) //parent is "dead"
        {
            if(livingOrigin.isRemoved())
            {
                this.discard();
            }
        }
        else if(age > Morph.configClient.acquisitionTendrilMaxChild * 10 + 100) //probably too long, kill it off
        {
            this.discard();

            if(livingOrigin instanceof Player)
            {
                // EntityBiomassAbility ability = Morph.EntityTypes.BIOMASS_ABILITY.create(this.level()).setInfo((Player)livingOrigin, 10, 0);
                // ((ClientLevel)this.level()).addEntity(ability);
            }
        }
        else //parent is "alive" and safe
        {
            this.setPos(livingOrigin.getX(), livingOrigin.getY() + (livingOrigin.getDimensions(Pose.STANDING).height() / 2D), livingOrigin.getZ());
            this.setYRot(livingOrigin.getYRot());
            this.setXRot(livingOrigin.getXRot());

            boolean allDone = !tendrils.isEmpty();
            boolean anyRetracting = false;
            boolean anyNonRetracting = false;
            for(Tendril tendril : tendrils)
            {
                if(!tendril.isDone())
                {
                    allDone = false;
                    tendril.tick();

                    if(tendril.retract)
                    {
                        anyRetracting = true;
                    }
                    else
                    {
                        anyNonRetracting = true;
                    }
                }
                else
                {
                    anyRetracting = true;
                }
            }

            tendrils.removeIf(Tendril::isDone);
            if (tendrils.isEmpty() && acquiredCapture.infos.isEmpty()) {
                allDone = true;
            }

            if(isMorphAcquisition)
            {
                if((!acquiredCapture.infos.isEmpty() || !hasCaptured) && age % 2 == 0 && tendrils.size() < (hasCaptured ? maxRequiredTendrils : 64))
                {
                    tendrils.add(new Tendril(null).headTowards(getTargetPos(!acquiredCapture.infos.isEmpty()), false));
                    allDone = false;//do not remove, we're not done yet
                }
                
                if(hasCaptured && acquiredCapture.infos.isEmpty())
                {
                    for(Tendril tendril : tendrils)
                    {
                        if(!tendril.retract) {
                            tendril.propagateRetractToChild();
                        }
                    }
                }
            }
            else
            {
                if(tendrils.size() < maxRequiredTendrils && !acquiredCapture.infos.isEmpty() && age % 3 == 0)
                {
                    tendrils.add(new Tendril(null).headTowards(getTargetPos(), true));
                    allDone = false;//do not remove, we're not done yet
                }
            }

            if(allDone && acquiredCapture.infos.isEmpty())
            {
                this.discard();

                if(livingOrigin instanceof Player)
                {
                    // EntityBiomassAbility ability = Morph.EntityTypes.BIOMASS_ABILITY.create(this.level()).setInfo((Player)livingOrigin, 10, 0);
                    // ((ClientLevel)this.level()).addEntity(ability);
                }
            }
        }
    }

    public Vec3 getTargetPos(boolean useParts)
    {
        if(!useParts)
        {
            // Randomly target the volume of the entity for modded entities without standard parts
            AABB box = livingAcquired.getDimensions(Pose.STANDING).makeBoundingBox(livingAcquired.position());
            double x = box.minX + (box.maxX - box.minX) * random.nextDouble();
            double y = box.minY + (box.maxY - box.minY) * random.nextDouble();
            double z = box.minZ + (box.maxZ - box.minZ) * random.nextDouble();
            return new Vec3(x, y, z);
        }
        return livingAcquired.position().add(0D, livingAcquired.getDimensions(Pose.STANDING).height() / 2D, 0D);
    }

    public Vec3 getTargetPos()
    {
        return getTargetPos(true);
    }

    public AABB getRenderBoundingBox()
    {
        return livingOrigin.getBoundingBox().minmax(livingAcquired.getBoundingBox());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return livingOrigin.shouldRenderAtSqrDistance(distance) || livingAcquired.shouldRenderAtSqrDistance(distance);
    }

    @Override
    public float getLightLevelDependentMagicValue()
    {
        return livingOrigin.getLightLevelDependentMagicValue();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){}

    @Override
    public boolean shouldRender(double x, double y, double z)
    {
        return livingOrigin.shouldRender(x, y, z);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input){}

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output){}

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity)
    {
        return null; // stub
    }

    public void syncWithOriginPosition()
    {
        double height = (livingOrigin.getDimensions(Pose.STANDING).height() / 2D);
        this.setPos(livingOrigin.getX(), livingOrigin.getY() + height, livingOrigin.getZ());
        this.setYRot(livingOrigin.getYRot());
        this.setXRot(livingOrigin.getXRot());

        this.xo = livingOrigin.xo;
        this.yo = livingOrigin.yo + height;
        this.zo = livingOrigin.zo;

        this.xOld = livingOrigin.xOld;
        this.yOld = livingOrigin.yOld + height;
        this.zOld = livingOrigin.zOld;
    }

    public class Tendril
    {
        @Nullable
        private Tendril parent; //if null, is the base tendril

        private Tendril child;
        private Vec3 offset;
        private float yaw;
        private float pitch;
        private float lastHeight = 1F;
        private float height = 1F;

        private float maxGrowth = 7F + (float)random.nextGaussian() * 2F;

        private boolean retract;
        private int retractTime;

        private float prevRotateSpin;
        private float rotateSpin;
        private float spinFactor = (float)random.nextGaussian() * 15F;

        public int depth = 0;

        private MorphRenderHandler.ModelPartCapture capture;

        public Tendril(Tendril parent)
        {
            this.parent = parent;
            if(parent != null)
            {
                this.offset = parent.getReachOffset().subtract(EntityAcquisition.this.calculateViewVector(parent.pitch, parent.yaw).multiply(0.025F, 0.025F, 0.025F));
                this.depth = parent.depth + 1;
            }
            else
            {
                this.offset = new Vec3(0D, 0D, 0D);
            }
        }

        public Tendril headTowards(Vec3 pos, boolean rev)
        {
            float randGaus = 5F;
            if(parent != null)
            {
                yaw = parent.yaw;
                pitch = parent.pitch;

                Vec3 origin = parent.getReachCoord();
                double d0 = pos.x - origin.x;
                double d1 = pos.y - origin.y;
                double d2 = pos.z - origin.z;

                float maxChange = isMorphAcquisition ? 45F : 60F;

                double dist = Mth.sqrt((float)(d0 * d0 + d2 * d2));
                float newYaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                float newPitch = (float)(-(Mth.atan2(d1, dist) * (double)(180F / (float)Math.PI)));
                this.pitch = Mth.approachDegrees(this.pitch, newPitch, maxChange);
                this.yaw = Mth.approachDegrees(this.yaw, newYaw, maxChange);
            }
            else
            {
                if(isMorphAcquisition)
                {
                    Vec3 pos2 = getTargetPos();
                    double d0 = pos2.x - EntityAcquisition.this.getX();
                    double d1 = pos2.y - EntityAcquisition.this.getY();
                    double d2 = pos2.z - EntityAcquisition.this.getZ();

                    double dist = Mth.sqrt((float)(d0 * d0 + d2 * d2));
                    yaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                    pitch = (float)(-(Mth.atan2(d1, dist) * (double)(180F / (float)Math.PI)));
                    
                    yaw += (2F + 8F * random.nextFloat()) * (random.nextBoolean() ? 1F : -1F);
                    pitch += (float)random.nextGaussian() * 3F;
                }
                else
                {
                    yaw = (rev ? (livingOrigin.yBodyRot + 180F) : livingOrigin.yBodyRot) % 360F;
                    pitch = 0;
                    yaw += (5F + 25F * random.nextFloat()) * (random.nextBoolean() ? 1F : -1F);
                    pitch += (float)random.nextGaussian() * 15F;
                }
            }

            return this;
        }

        public void tick()
        {
            lastHeight = height;
            if(retract)
            {
                retractTime++;
            }

            if(child != null)
            {
                child.tick();
            }
            else
            {
                float distToEnt = livingOrigin.distanceTo(livingAcquired);
                if(!isMorphAcquisition)
                {
                    distToEnt *= 2F;
                }
                if(!retract)
                {
                    if(height < maxGrowth)
                    {
                        float maxTendrilGrowth = Math.max(0.0625F, distToEnt / Morph.configClient.acquisitionTendrilMaxChild + (float)random.nextGaussian() * 0.125F); //in blocks
                        height += maxTendrilGrowth * 16F;
                        if(getReachCoord().distanceTo(getTargetPos(!acquiredCapture.infos.isEmpty())) < Math.max(0.5F, maxTendrilGrowth)) //close enough?
                        {
                            if(!isMorphAcquisition && age <= 10)
                            {
                                height -= maxTendrilGrowth * 16F; //wait for age to finish
                                return;
                            }

                            child = new Tendril(this);

                            Vec3 pos = getTargetPos(!acquiredCapture.infos.isEmpty());
                            Vec3 origin = getReachCoord();
                            double d0 = pos.x - origin.x;
                            double d1 = pos.y - origin.y;
                            double d2 = pos.z - origin.z;

                            double dist = Mth.sqrt((float)(d0 * d0 + d2 * d2));
                            child.yaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                            child.pitch = (float)(-(Mth.atan2(d1, dist) * (double)(180F / (float)Math.PI)));
                            child.lastHeight = child.height = (float)Mth.sqrt((float)(d0 * d0 + d1 * d1 + d2 * d2)) * 16F;

                            if(isMorphAcquisition && !acquiredCapture.infos.isEmpty())
                            {
                                child.capture = new MorphRenderHandler.ModelPartCapture();
                                int count = (int)Math.ceil(Math.max(acquiredCapture.infos.size() / 5F, 1));
                                for(int x = 0; x < count && !acquiredCapture.infos.isEmpty(); x++)
                                {
                                    int i = random.nextInt(acquiredCapture.infos.size());
                                    child.capture.infos.add(acquiredCapture.infos.get(i));
                                    acquiredCapture.infos.remove(i);
                                }
                            }
                            else if(!acquiredCapture.infos.isEmpty())
                            {
                                child.capture = new MorphRenderHandler.ModelPartCapture();
                                int count = (int)Math.ceil(Math.max(acquiredCapture.infos.size() / 10F, 1));
                                for(int x = 0; x < count && !acquiredCapture.infos.isEmpty(); x++)
                                {
                                    int i = random.nextInt(acquiredCapture.infos.size());
                                    child.capture.infos.add(acquiredCapture.infos.get(i));
                                    acquiredCapture.infos.remove(i);
                                    if(x > 0)
                                    {
                                        maxRequiredTendrils--;
                                    }
                                }
                            }

                            child.propagateRetractToParent();
                        }

                        if(!isMorphAcquisition && acquiredCapture.infos.isEmpty()) //oops we're out of blocks. retract
                        {
                            propagateRetractToParent();
                        }
                    }
                    else
                    {
                        child = new Tendril(this).headTowards(getTargetPos(), false);
                    }
                }
                else if(retractTime <= 3)
                {
                    float maxTendrilGrowth = Math.max(0.0625F, distToEnt / Morph.configClient.acquisitionTendrilMaxChild + (float)random.nextGaussian() * 0.125F); //in blocks
                    if(getReachCoord().distanceTo(getTargetPos()) > Math.max(0.5F, maxTendrilGrowth))
                    {
                        Vec3 pos = getTargetPos();
                        Vec3 origin = getReachCoord();
                        double d0 = pos.x - origin.x;
                        double d1 = pos.y - origin.y;
                        double d2 = pos.z - origin.z;

                        double dist = Mth.sqrt((float)(d0 * d0 + d2 * d2));
                        yaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                        pitch = (float)(-(Mth.atan2(d1, dist) * (double)(180F / (float)Math.PI)));

                        height += maxTendrilGrowth * 16F;
                    }
                }
                else if(height > 0 && retractTime > 6)
                {
                    prevRotateSpin = rotateSpin;
                    if(capture != null)
                    {
                        rotateSpin += spinFactor;
                    }

                    float maxTendrilGrowth = Math.max(0.0625F, distToEnt / (Morph.configClient.acquisitionTendrilMaxChild * 2F) + (float)random.nextGaussian() * 0.125F); //in blocks
                    height -= maxTendrilGrowth * 16F;
                    if(height <= 0)
                    {
                        height = 0;

                        if(parent != null)
                        {
                            parent.child = null; //remove ourselves.
                            if(capture != null)
                            {
                                parent.capture = capture;
                                parent.prevRotateSpin = prevRotateSpin;
                                parent.rotateSpin = rotateSpin;
                            }
                        }
                    }
                }
            }
        }

        public boolean isDone()
        {
            return retract && height <= 0F;
        }

        public void propagateRetractToParent()
        {
            retract = true;
            if(parent != null)
            {
                parent.propagateRetractToParent();
            }
        }

        public void propagateRetractToChild()
        {
            retract = true;
            if(child != null)
            {
                child.propagateRetractToChild();
            }
        }

        public Vec3 getReachOffset()
        {
            float growth = height / 16F;
            return offset.add(EntityAcquisition.this.calculateViewVector(pitch, yaw).multiply(growth, growth, growth));
        }

        public Vec3 getReachCoord()
        {
            return EntityAcquisition.this.position().add(getReachOffset());
        }

        public float getWidth(float partialTick)
        {
            float width = 1F + (0.2F * remainingDepth(partialTick));
            if(width > 3.5F)
            {
                width = 3.5F;
            }
            return width;
        }

        public float remainingDepth(float partialTick)
        {
            int depth = 0;
            Tendril aParent = this;
            Tendril aChild = aParent.child;

            while(aChild != null)
            {
                depth += Math.min((aChild.lastHeight + (aChild.height - aChild.lastHeight) * partialTick) / aChild.maxGrowth, 1F);

                aParent = aChild;
                aChild = aParent.child;
            }

            return depth;
        }

        public void renderTendril(EntityAcquisition entity, PoseStack stack, VertexConsumer buffer, int light, int overlay, float partialTick)
        {
            if(child != null)
            {
                child.renderTendril(entity, stack, buffer, light, overlay, partialTick);
            }

            float width = getWidth(partialTick) / 16F;
            float halfWidth = width / 2F;
            float len = (lastHeight + (height - lastHeight) * partialTick) / 16F;
            
            if (len > 0) {
                float alpha = 1F;
                if(entity.livingOrigin == net.minecraft.client.Minecraft.getInstance().cameraEntity && net.minecraft.client.Minecraft.getInstance().options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON)
                {
                    alpha = Mth.clamp((depth + 1) / (float)Morph.configClient.acquisitionTendrilPartOpacity, 0F, 1F);
                }

                stack.pushPose();
                stack.translate(offset.x, offset.y, offset.z);
                stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
                stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
                
                // Draw a simple box (Textured)
                renderBox(stack, buffer, -halfWidth, -halfWidth, 0, halfWidth, halfWidth, len, 1F, 1F, 1F, alpha, light, overlay);

                stack.popPose();
            }
        }

        private void renderBox(PoseStack stack, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a, int light, int overlay) {
            org.joml.Matrix4f pose = stack.last().pose();
            org.joml.Matrix3f normal = stack.last().normal();

            float w = (maxX - minX) * 16F;
            float h = (maxY - minY) * 16F;
            float d = (maxZ - minZ) * 16F;

            // standard Texture UV mapping logic (using 0,0 as offset) / 64 for width, 32 for height
            float texW = 64F;
            float texH = 32F;

            // X offsets
            float rightX = 0;
            float frontX = d;
            float leftX = d + w;
            float backX = d + w + d;

            // Y offsets
            float topY = 0;
            float sideY = d;

            // top face
            float u0 = frontX / texW, v0 = topY / texH, u1 = (frontX + w) / texW, v1 = (topY + d) / texH;
            addVertex(pose, normal, buffer, minX, maxY, maxZ, r, g, b, a, u0, v1, light, overlay, 0, 1, 0);
            addVertex(pose, normal, buffer, maxX, maxY, maxZ, r, g, b, a, u1, v1, light, overlay, 0, 1, 0);
            addVertex(pose, normal, buffer, maxX, maxY, minZ, r, g, b, a, u1, v0, light, overlay, 0, 1, 0);
            addVertex(pose, normal, buffer, minX, maxY, minZ, r, g, b, a, u0, v0, light, overlay, 0, 1, 0);

            // bottom face
            u0 = leftX / texW; v0 = topY / texH; u1 = (leftX + w) / texW; v1 = (topY + d) / texH;
            addVertex(pose, normal, buffer, minX, minY, minZ, r, g, b, a, u0, v0, light, overlay, 0, -1, 0);
            addVertex(pose, normal, buffer, maxX, minY, minZ, r, g, b, a, u1, v0, light, overlay, 0, -1, 0);
            addVertex(pose, normal, buffer, maxX, minY, maxZ, r, g, b, a, u1, v1, light, overlay, 0, -1, 0);
            addVertex(pose, normal, buffer, minX, minY, maxZ, r, g, b, a, u0, v1, light, overlay, 0, -1, 0);

            // right face
            u0 = rightX / texW; v0 = sideY / texH; u1 = frontX / texW; v1 = (sideY + h) / texH;
            addVertex(pose, normal, buffer, minX, maxY, minZ, r, g, b, a, u0, v0, light, overlay, -1, 0, 0);
            addVertex(pose, normal, buffer, minX, maxY, maxZ, r, g, b, a, u1, v0, light, overlay, -1, 0, 0);
            addVertex(pose, normal, buffer, minX, minY, maxZ, r, g, b, a, u1, v1, light, overlay, -1, 0, 0);
            addVertex(pose, normal, buffer, minX, minY, minZ, r, g, b, a, u0, v1, light, overlay, -1, 0, 0);

            // front face
            u0 = frontX / texW; v0 = sideY / texH; u1 = leftX / texW; v1 = (sideY + h) / texH;
            addVertex(pose, normal, buffer, minX, maxY, maxZ, r, g, b, a, u0, v0, light, overlay, 0, 0, 1);
            addVertex(pose, normal, buffer, minX, minY, maxZ, r, g, b, a, u0, v1, light, overlay, 0, 0, 1);
            addVertex(pose, normal, buffer, maxX, minY, maxZ, r, g, b, a, u1, v1, light, overlay, 0, 0, 1);
            addVertex(pose, normal, buffer, maxX, maxY, maxZ, r, g, b, a, u1, v0, light, overlay, 0, 0, 1);

            // left face
            u0 = leftX / texW; v0 = sideY / texH; u1 = backX / texW; v1 = (sideY + h) / texH;
            addVertex(pose, normal, buffer, maxX, maxY, maxZ, r, g, b, a, u0, v0, light, overlay, 1, 0, 0);
            addVertex(pose, normal, buffer, maxX, minY, maxZ, r, g, b, a, u0, v1, light, overlay, 1, 0, 0);
            addVertex(pose, normal, buffer, maxX, minY, minZ, r, g, b, a, u1, v1, light, overlay, 1, 0, 0);
            addVertex(pose, normal, buffer, maxX, maxY, minZ, r, g, b, a, u1, v0, light, overlay, 1, 0, 0);

            // back face
            u0 = backX / texW; v0 = sideY / texH; u1 = (backX + w) / texW; v1 = (sideY + h) / texH;
            addVertex(pose, normal, buffer, maxX, maxY, minZ, r, g, b, a, u0, v0, light, overlay, 0, 0, -1);
            addVertex(pose, normal, buffer, maxX, minY, minZ, r, g, b, a, u0, v1, light, overlay, 0, 0, -1);
            addVertex(pose, normal, buffer, minX, minY, minZ, r, g, b, a, u1, v1, light, overlay, 0, 0, -1);
            addVertex(pose, normal, buffer, minX, maxY, minZ, r, g, b, a, u1, v0, light, overlay, 0, 0, -1);
        }

        private void addVertex(org.joml.Matrix4f pose, org.joml.Matrix3f normal, VertexConsumer buffer, float x, float y, float z, float r, float g, float b, float a, float u, float v, int light, int overlay, float nx, float ny, float nz) {
            org.joml.Vector3f transformedNormal = normal.transform(new org.joml.Vector3f(nx, ny, nz));
            buffer.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
        }

        public void renderCapture(EntityAcquisition acquisition, PoseStack matrixStack, VertexConsumer vertexBuilder, int light, int overlay, float partialTick)
        {
            if(child != null)
            {
                child.renderCapture(acquisition, matrixStack, vertexBuilder, light, overlay, partialTick);
            }
            else if(capture != null) //only at tendril endpoints.
            {
                float renderHeight = lastHeight + (height - lastHeight) * partialTick;
                float heightOffset = renderHeight / 16F;
                Vec3 look = EntityAcquisition.this.calculateViewVector(pitch, yaw);
                Vec3 renderPoint = offset.add(look.multiply(heightOffset, heightOffset, heightOffset));

                float alpha = 1F;
                if(acquisition.livingOrigin == net.minecraft.client.Minecraft.getInstance().cameraEntity && net.minecraft.client.Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON)
                {
                    alpha = Mth.clamp((depth + 1) / (float)Morph.configClient.acquisitionTendrilPartOpacity, 0F, 1F);
                }

                if(alpha > 0F)
                {
                    float scale;
                    double distToEnt = acquisition.livingOrigin.distanceTo(acquisition.livingAcquired);
                    double distToRenderPoint = Mth.sqrt((float)acquisition.distanceToSqr(acquisition.position().add(renderPoint)));
                    if(distToEnt > 0D)
                    {
                        scale = (float)(Math.min(distToEnt, (distToRenderPoint + 0.5D)) / distToEnt); //+1 to make the render still show the entity slightly as it's being pulled in.
                    }
                    else
                    {
                        scale = 0F;
                    }

                    matrixStack.pushPose();
                    matrixStack.translate(renderPoint.x, renderPoint.y, renderPoint.z);
                    matrixStack.scale(scale, scale, scale);
                    float rot = prevRotateSpin + (rotateSpin - prevRotateSpin) * partialTick;
                    matrixStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rot));
                    // stack.rotate(Vector3f.YP.rotationDegrees(rot));
                    matrixStack.translate(0D, -(acquisition.livingAcquired.getDimensions(net.minecraft.world.entity.Pose.STANDING).height() / 2D), 0D);
                    if(isMorphAcquisition)
                    {
                        capture.render(matrixStack, vertexBuilder, light, overlay, 1F, 1F, 1F, alpha);
                    }
                    else
                    {
                        for(MorphRenderHandler.CaptureInfo info : capture.infos)
                        {
                            PoseStack identityStack = new PoseStack();
                            matrixStack.pushPose();
                            PoseStack.Pose e = RenderHelper.createInterimStackEntry(identityStack.last(), info.e, Mth.clamp(scale * 3F, 0F, 1F));
                            PoseStack.Pose last = matrixStack.last();
                            last.pose().mul(e.pose());
                            last.normal().mul(e.normal());
                            int captureColor = net.minecraft.util.ARGB.color((int)(alpha * 255F), 255, 255, 255);
                            info.createAndRender(matrixStack, vertexBuilder, light, overlay, captureColor);
                            matrixStack.popPose();
                        }
                    }
                    matrixStack.popPose();
                }
            }
        }
    }
}
