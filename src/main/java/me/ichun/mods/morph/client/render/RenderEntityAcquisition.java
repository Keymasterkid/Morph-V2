package me.ichun.mods.morph.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.morph.client.entity.EntityAcquisition;
import me.ichun.mods.morph.client.model.ModelAcquisition;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderEntityAcquisition extends EntityRenderer<EntityAcquisition>
{
    private final ModelAcquisition model;

    public RenderEntityAcquisition(EntityRendererProvider.Context context)
    {
        super(context);
        model = new ModelAcquisition();
        shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityAcquisition acquisition, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource buffer, int light)
    {
        if(acquisition.acquiredCapture != null)
        {
            EntityRenderer<? super LivingEntity> renderer = this.entityRenderDispatcher.getRenderer(acquisition.livingAcquired);
            Vec3 renderOffset = renderer.getRenderOffset(acquisition.livingAcquired, partialTicks);

            //calculate the difference of that entity from ours
            double d0 = Mth.lerp(partialTicks, acquisition.livingAcquired.xo - acquisition.xo, acquisition.livingAcquired.getX() - acquisition.getX()) + renderOffset.x;
            double d1 = Mth.lerp(partialTicks, acquisition.livingAcquired.yo - acquisition.yo, acquisition.livingAcquired.getY() - acquisition.getY()) + renderOffset.y;
            double d2 = Mth.lerp(partialTicks, acquisition.livingAcquired.zo - acquisition.zo, acquisition.livingAcquired.getZ() - acquisition.getZ()) + renderOffset.z;

            if(!acquisition.hasCaptured && acquisition.tickCount < 60)
            {
                MorphRenderHandler.currentCapture = acquisition.acquiredCapture;
                acquisition.acquiredCapture.infos.clear();

                MorphRenderHandler.renderLiving(renderer, acquisition.livingAcquired, new PoseStack(), buffer, this.entityRenderDispatcher.getPackedLightCoords(acquisition.livingAcquired, partialTicks), partialTicks, Morph.configServer.biomassSkinWhilstInvisible);

                MorphRenderHandler.currentCapture = null;

                if(!acquisition.acquiredCapture.infos.isEmpty())
                {
                    acquisition.hasCaptured = true;
                    acquisition.maxRequiredTendrils = acquisition.acquiredCapture.infos.size();
                }
            }

            float skinAlpha = Mth.clamp((acquisition.tickCount + partialTicks) / 10, 0F, 1F);

            stack.pushPose();
            stack.translate(d0, d1, d2);
            acquisition.acquiredCapture.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, skinAlpha);
            stack.popPose();
        }
        model.render(acquisition, partialTicks, stack, buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(acquisition))), light, LivingEntityRenderer.getOverlayCoords(acquisition.livingOrigin, 0F));
    }

    @Override
    public boolean shouldRender(EntityAcquisition entity, Frustum camera, double camX, double camY, double camZ)
    {
        entity.syncWithOriginPosition();
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityAcquisition entity)
    {
        return MorphHandler.INSTANCE.getMorphSkinTexture();
    }

    public static class RenderFactory implements EntityRendererProvider<EntityAcquisition>
    {
        @Override
        public EntityRenderer<EntityAcquisition> create(EntityRendererProvider.Context context)
        {
            return new RenderEntityAcquisition(context);
        }
    }
}
