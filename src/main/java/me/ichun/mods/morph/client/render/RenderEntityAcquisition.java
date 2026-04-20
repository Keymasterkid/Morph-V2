package me.ichun.mods.morph.client.render;

import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.morph.client.entity.EntityAcquisition;
import me.ichun.mods.morph.client.model.ModelAcquisition;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderEntityAcquisition extends EntityRenderer<EntityAcquisition, RenderEntityAcquisition.AcquisitionRenderState>
{
    private final ModelAcquisition model;

    public RenderEntityAcquisition(EntityRendererProvider.Context context)
    {
        super(context);
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        model = new ModelAcquisition(LayerDefinition.create(meshdefinition, 64, 64).bakeRoot());
    }

    @Override
    public void render(AcquisitionRenderState state, PoseStack stack, MultiBufferSource buffer, int light)
    {
        if(state.acquiredCapture != null)
        {
            if(!state.hasCaptured && state.age < 60)
            {
                MorphRenderHandler.currentCapture = state.acquiredCapture;
                state.acquiredCapture.infos.clear();

                EntityRenderer renderer = this.entityRenderDispatcher.getRenderer(state.livingAcquired);
                MorphRenderHandler.renderLiving(renderer, state.livingAcquired, new PoseStack(), buffer, state.packedLight, state.partialTicks, Morph.configServer.biomassSkinWhilstInvisible);

                MorphRenderHandler.currentCapture = null;

                if(!state.acquiredCapture.infos.isEmpty())
                {
                    state.entity.hasCaptured = true;
                    state.entity.maxRequiredTendrils = state.acquiredCapture.infos.size();
                } else {
                    // Fallback for modded entities with custom renderers (e.g. GeckoLib)
                    state.entity.hasCaptured = true;
                    state.entity.maxRequiredTendrils = 10;
                }
            }

            float skinAlpha = Mth.clamp((state.age + state.partialTicks) / 10, 0F, 1F);

            stack.pushPose();
            stack.translate(state.renderOffset.x, state.renderOffset.y, state.renderOffset.z);
            state.acquiredCapture.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, skinAlpha);
            stack.popPose();
        }
        model.render(state.entity, state.partialTicks, stack, buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(state))), light, state.overlayCoords);
    }

    @Override
    public AcquisitionRenderState createRenderState() {
        return new AcquisitionRenderState();
    }

    @Override
    public void extractRenderState(EntityAcquisition entity, AcquisitionRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entity = entity;
        state.acquiredCapture = entity.acquiredCapture;
        state.livingAcquired = entity.livingAcquired;
        state.livingOrigin = entity.livingOrigin;
        state.hasCaptured = entity.hasCaptured;
        state.age = entity.age;
        state.partialTicks = partialTicks;
        if(entity.livingAcquired != null) {
            state.packedLight = this.entityRenderDispatcher.getPackedLightCoords(entity.livingAcquired, partialTicks);
        }
        if(entity.livingOrigin != null) {
            EntityRenderer renderer = this.entityRenderDispatcher.getRenderer(entity.livingOrigin);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                LivingEntityRenderState livingState = (LivingEntityRenderState)livingRenderer.createRenderState();
                livingRenderer.extractRenderState(entity.livingOrigin, livingState, partialTicks);
                state.overlayCoords = LivingEntityRenderer.getOverlayCoords(livingState, 0F);
            }
        }

        if(entity.acquiredCapture != null && entity.livingAcquired != null) {
            EntityRenderer renderer = this.entityRenderDispatcher.getRenderer(entity.livingAcquired);
            Vec3 offset = Vec3.ZERO;
            if(renderer instanceof LivingEntityRenderer livingRenderer) {
                LivingEntityRenderState tempState = (LivingEntityRenderState)livingRenderer.createRenderState();
                livingRenderer.extractRenderState(entity.livingAcquired, tempState, partialTicks);
                offset = livingRenderer.getRenderOffset(tempState);
            }
            double d0 = Mth.lerp(partialTicks, entity.livingAcquired.xo - entity.xo, entity.livingAcquired.getX() - entity.getX()) + offset.x;
            double d1 = Mth.lerp(partialTicks, entity.livingAcquired.yo - entity.yo, entity.livingAcquired.getY() - entity.getY()) + offset.y;
            double d2 = Mth.lerp(partialTicks, entity.livingAcquired.zo - entity.zo, entity.livingAcquired.getZ() - entity.getZ()) + offset.z;
            state.renderOffset = new Vec3(d0, d1, d2);
        } else {
            state.renderOffset = Vec3.ZERO;
        }
    }

    @Override
    public boolean shouldRender(EntityAcquisition entity, Frustum camera, double camX, double camY, double camZ)
    {
        entity.syncWithOriginPosition();
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }

    public ResourceLocation getTextureLocation(AcquisitionRenderState state)
    {
        return MorphHandler.INSTANCE.getMorphSkinTexture();
    }

    public static class AcquisitionRenderState extends EntityRenderState {
        public EntityAcquisition entity;
        public MorphRenderHandler.ModelPartCapture acquiredCapture;
        public LivingEntity livingAcquired;
        public LivingEntity livingOrigin;
        public boolean hasCaptured;
        public int age;
        public float partialTicks;
        public int packedLight;
        public int overlayCoords;
        public Vec3 renderOffset;
    }

    public static class RenderFactory implements EntityRendererProvider<EntityAcquisition>
    {
        @Override
        public EntityRenderer<EntityAcquisition, AcquisitionRenderState> create(EntityRendererProvider.Context context)
        {
            return new RenderEntityAcquisition(context);
        }
    }
}
