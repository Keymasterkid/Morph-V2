package me.ichun.mods.morph.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.morph.client.entity.EntityBiomassAbility;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.MorphInfoImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class RenderEntityBiomassAbility extends EntityRenderer<EntityBiomassAbility, RenderEntityBiomassAbility.BiomassAbilityRenderState>
{
    public RenderEntityBiomassAbility(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void render(BiomassAbilityRenderState state, PoseStack stack, MultiBufferSource buffer, int light)
    {
        if(state.isRemoved || state.isFirstPerson)
        {
            return;
        }

        if(state.activeLiving != null)
        {
            EntityRenderer<?, ?> renderer = this.entityRenderDispatcher.getRenderer(state.activeLiving);
            if(renderer != null)
            {
                MorphRenderHandler.denyRenderNameplate = true;
                stack.pushPose();
                MorphRenderHandler.renderLiving(renderer, state.activeLiving, stack, buffer, state.packedLight, state.partialTick);
                stack.popPose();

                MorphRenderHandler.currentCapture = state.capture;
                MorphRenderHandler.currentCapture.infos.clear();

                MorphRenderHandler.renderLiving(renderer, state.activeLiving, new PoseStack(), buffer, state.packedLight, state.partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                MorphRenderHandler.currentCapture = null;
                MorphRenderHandler.denyRenderNameplate = false;

                state.capture.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, state.alpha);
            }
        }
    }

    @Override
    public BiomassAbilityRenderState createRenderState() {
        return new BiomassAbilityRenderState();
    }

    @Override
    public void extractRenderState(EntityBiomassAbility entity, BiomassAbilityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isRemoved = entity.player.isRemoved();
        state.partialTick = partialTicks;
        
        MorphInfoImpl info = (MorphInfoImpl)MorphHandler.INSTANCE.getMorphInfo(entity.player);
        state.isFirstPerson = entity.player == Minecraft.getInstance().cameraEntity && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
        if(state.isFirstPerson)
        {
            if(info.entityBiomassAbility == null || info.entityBiomassAbility.getSkinAlpha(partialTicks) < entity.getSkinAlpha(partialTicks))
            {
                info.entityBiomassAbility = entity;
            }
        }

        state.activeLiving = info.getActiveAppearanceEntity(partialTicks);
        if(state.activeLiving != null) {
            state.packedLight = this.entityRenderDispatcher.getPackedLightCoords(state.activeLiving, partialTicks);
            state.alpha = entity.getSkinAlpha(partialTicks);
            state.capture = entity.capture;
        }
    }

    @Override
    public boolean shouldRender(EntityBiomassAbility ability, Frustum camera, double camX, double camY, double camZ)
    {
        ability.syncWithOriginPosition();
        return super.shouldRender(ability, camera, camX, camY, camZ);
    }

    public ResourceLocation getTextureLocation(BiomassAbilityRenderState state)
    {
        return MorphHandler.INSTANCE.getMorphSkinTexture();
    }

    public static class BiomassAbilityRenderState extends EntityRenderState {
        public boolean isRemoved;
        public boolean isFirstPerson;
        public LivingEntity activeLiving;
        public float partialTick;
        public int packedLight;
        public float alpha;
        public MorphRenderHandler.ModelPartCapture capture;
    }

    public static class RenderFactory implements EntityRendererProvider<EntityBiomassAbility>
    {
        @Override
        public EntityRenderer<EntityBiomassAbility, BiomassAbilityRenderState> create(EntityRendererProvider.Context context)
        {
            return new RenderEntityBiomassAbility(context);
        }
    }
}
