package me.ichun.mods.morph.client.render;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.client.model.util.ModelHelper;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.ichunutil.common.module.tabula.project.Project;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.MorphInfoImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class MorphRenderHandler
{
    private static VertexConsumer createNoOpVertexConsumer()
    {
        return new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
            @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
            @Override public VertexConsumer setUv(float u, float v) { return this; }
            @Override public VertexConsumer setUv1(int u, int v) { return this; }
            @Override public VertexConsumer setUv2(int u, int v) { return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
            @Override public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) { }
        };
    }

    private static float playerShadowSize = -1F;
    private static boolean changedShadowSize = false;

    public static ModelPartCapture currentCapture = null; //Are we capturing ModelPart renders?

    public static boolean isRenderingMorph = false;
    public static boolean denyRenderNameplate = false;

    public static void renderMorphInfo(Player player, MorphInfoImpl info, PoseStack stack, MultiBufferSource buffer, int light, float partialTick)
    {
        isRenderingMorph = true;

        float morphProgress = info.getMorphProgress(partialTick);

        if(morphProgress < 1F) //still morphing
        {
            float skinProg = 1F;
            float transitionProgress = info.getTransitionProgressSine(partialTick);
            if(transitionProgress <= 0F)
            {
                LivingEntity entInstance = info.prevState.getEntityInstance(player.level(), player);
                UUID morphUniqueId = entInstance.getUUID();
                entInstance.setUUID(player.getUUID());
                MorphState.syncEntityWithPlayer(entInstance, player);
                renderLiving(info.prevState, entInstance, stack, buffer, light, partialTick);
                entInstance.setUUID(morphUniqueId);
                skinProg = EntityHelper.sineifyProgress(morphProgress / 0.125F);
            }
            else if(transitionProgress >= 1F)
            {
                LivingEntity entInstance = info.nextState.getEntityInstance(player.level(), player);
                UUID morphUniqueId = entInstance.getUUID();
                entInstance.setUUID(player.getUUID());
                MorphState.syncEntityWithPlayer(entInstance, player);
                renderLiving(info.nextState, entInstance, stack, buffer, light, partialTick);
                entInstance.setUUID(morphUniqueId);
                skinProg = 1F - EntityHelper.sineifyProgress((morphProgress - 0.875F) / 0.125F);
            }

            int overlay = OverlayTexture.NO_OVERLAY; // LivingEntityRenderer.getPackedOverlay(player, 0.0F); 
            renderTransitionState(player, info, stack, buffer, light, overlay, partialTick, transitionProgress, skinProg);
        }
        else //has completed morph
        {
            LivingEntity entInstance = info.nextState.getEntityInstance(player.level(), player);
            UUID morphUniqueId = entInstance.getUUID();
            entInstance.setUUID(player.getUUID());
            MorphState.syncEntityWithPlayer(entInstance, player);
            entInstance.setUUID(morphUniqueId);
            renderLiving(info.nextState, entInstance, stack, buffer, light, partialTick);
        }

        isRenderingMorph = false;
    }

    private static void renderLiving(MorphState state, LivingEntity living, PoseStack stack, MultiBufferSource buffer, int light, float partialTick) //also captures the shadow size
    {
        renderLiving(state, living, stack, buffer, light, partialTick, false);
    }

    private static void renderLiving(MorphState state, LivingEntity living, PoseStack stack, MultiBufferSource buffer, int light, float partialTick, boolean forceDuringInvisibility) //also captures the shadow size
    {
        EntityRenderer<?, ?> livingRenderer = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
        if(livingRenderer != null)
        {
            renderLiving(livingRenderer, living, stack, buffer, light, partialTick, forceDuringInvisibility);
            if (living instanceof Mob && living.isBaby())
            {
                state.renderedShadowSize = MorphRenderHelper.getShadowRadius(livingRenderer) * 0.5F;
            }
            else
            {
                state.renderedShadowSize = MorphRenderHelper.getShadowRadius(livingRenderer);
            }
        }
    }

    public static void renderLiving(EntityRenderer<?, ?> renderer, LivingEntity living, PoseStack stack, MultiBufferSource buffer, int light, float partialTick, boolean forceDuringInvisibility)
    {
        boolean isInvisible = living.isInvisible();
        if(forceDuringInvisibility && isInvisible)
        {
            living.setInvisible(false);
        }
        renderLiving(renderer, living, stack, buffer, light, partialTick);
        if(forceDuringInvisibility && isInvisible)
        {
            living.setInvisible(true);
        }
    }

    @SuppressWarnings("unchecked")
    public static void renderLiving(EntityRenderer renderer, LivingEntity living, PoseStack stack, MultiBufferSource buffer, int light, float partialTick)
    {
        Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if(living instanceof LocalPlayer)
        {
            LocalPlayer player = (LocalPlayer)living;
        }

        renderer.render(renderer.createRenderState(living, partialTick), stack, buffer, light);
    }

    public static void renderTransitionState(Player player, MorphInfoImpl info, PoseStack stack, MultiBufferSource buffer, int light, int overlay, float partialTick, float transitionProgress, float skinAlpha)
    {
        if(info.transitionState == null)
        {
            info.transitionState = new MorphTransitionState();
        }

        info.transitionState.renderTransitionState(player, info, stack, buffer, light, overlay, partialTick, transitionProgress, skinAlpha);
    }

    public static void restoreShadowSize(PlayerRenderer renderer)
    {
        if(playerShadowSize == -1F)
        {
            playerShadowSize = MorphRenderHelper.getShadowRadius(renderer);
        }

        if(changedShadowSize)
        {
            changedShadowSize = false;
            MorphRenderHelper.setShadowRadius(renderer, playerShadowSize);
        }
    }

    public static void setShadowSize(PlayerRenderer renderer, MorphInfo info, float partialTick)
    {
        float morphProgress = info.getMorphProgress(partialTick);
        if(morphProgress < 1F) //midmorph
        {
            float prevSize = info.prevState.renderedShadowSize;
            float nextSize = info.nextState.renderedShadowSize;

            MorphRenderHelper.setShadowRadius(renderer, prevSize + (nextSize - prevSize) * info.getTransitionProgressSine(partialTick));
        }
        else
        {
            MorphRenderHelper.setShadowRadius(renderer, info.nextState.renderedShadowSize);
        }

        changedShadowSize = true;
    }

    public static PoseStack.Pose createPose(org.joml.Matrix4f p, org.joml.Matrix3f n) {
        PoseStack.Pose pose = new PoseStack.Pose();
        pose.pose().set(p);
        pose.normal().set(n);
        return pose;
    }

    public static class MorphTransitionState
    {
        protected ModelPartCapture prevModel;
        protected ModelPartCapture nextModel;

        public void renderTransitionState(Player player, MorphInfo info, PoseStack stack, MultiBufferSource buffer, int light, int overlay, float partialTick, float transitionProgress, float skinAlpha)
        {
            if(transitionProgress <= 0F)
            {
                if(prevModel == null)
                {
                    currentCapture = prevModel = new ModelPartCapture();
                }
                else
                {
                    currentCapture = prevModel;
                    currentCapture.infos.clear();
                }

                LivingEntity livingInstance = info.prevState.getEntityInstance(player.level(), player);

                renderLiving(info.prevState, livingInstance, new PoseStack(), (rt) -> createNoOpVertexConsumer(), light, partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                currentCapture = null; //reset before we do anything else

                prevModel.render(stack, buffer, light, overlay, skinAlpha);
            }
            else if(transitionProgress >= 1F)
            {
                if(nextModel == null)
                {
                    currentCapture = nextModel = new ModelPartCapture();
                }
                else
                {
                    currentCapture = nextModel;
                    currentCapture.infos.clear();
                }

                LivingEntity livingInstance = info.nextState.getEntityInstance(player.level(), player);

                renderLiving(info.nextState, livingInstance, new PoseStack(), (rt) -> createNoOpVertexConsumer(), light, partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                currentCapture = null; //reset before we do anything else

                nextModel.render(stack, buffer, light, overlay, skinAlpha);
            }
            else
            {
                denyRenderNameplate = true;
                if(prevModel == null)
                {
                    currentCapture = prevModel = new ModelPartCapture();
                }
                else
                {
                    currentCapture = prevModel;
                    currentCapture.infos.clear();
                }

                LivingEntity prevLivingInstance = info.prevState.getEntityInstance(player.level(), player);

                renderLiving(info.prevState, prevLivingInstance, new PoseStack(), (rt) -> createNoOpVertexConsumer(), light, partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                if(nextModel == null)
                {
                    currentCapture = nextModel = new ModelPartCapture();
                }
                else
                {
                    currentCapture = nextModel;
                    currentCapture.infos.clear();
                }

                LivingEntity nextLivingInstance = info.nextState.getEntityInstance(player.level(), player);

                renderLiving(info.nextState, nextLivingInstance, new PoseStack(), (rt) -> createNoOpVertexConsumer(), light, partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                currentCapture = null; //reset before we do anything else
                denyRenderNameplate = false;

                stack.pushPose();
                stack.translate(0F, prevLivingInstance.getDimensions(net.minecraft.world.entity.Pose.STANDING).height() / 2F, 0F);
                PoseStack.Pose prevMid = MorphRenderHandler.createPose(new org.joml.Matrix4f(stack.last().pose()), new org.joml.Matrix3f(stack.last().normal()));
                stack.popPose();

                stack.pushPose();
                stack.translate(0F, nextLivingInstance.getDimensions(net.minecraft.world.entity.Pose.STANDING).height() / 2F, 0F);
                PoseStack.Pose nextMid = MorphRenderHandler.createPose(new org.joml.Matrix4f(stack.last().pose()), new org.joml.Matrix3f(stack.last().normal()));
                stack.popPose();

                ModelPartCapture transitionCapture = new ModelPartCapture();
                transitionCapture.infos = prevModel.combineTowards(prevMid, nextMid, nextModel, transitionProgress);

                transitionCapture.render(stack, buffer, light, overlay, skinAlpha);
            }
        }
    }

    public static class ModelPartCapture
    {
        private final HashMap<net.minecraft.client.model.geom.ModelPart, CaptureInfo.ModelPart> modelToPart = new HashMap<>();

        public ArrayList<CaptureInfo> infos = new ArrayList<>();

        public void capture(net.minecraft.client.model.geom.ModelPart renderer, PoseStack stack)
        {
            if(modelToPart.containsKey(renderer))
            {
                infos.add(new CaptureInfo(stack.last(), modelToPart.get(renderer)));
            }
            else
            {
                Project.Part part = ModelHelper.createPartFor(renderer, false);
                part.rotPX = part.rotPY = part.rotPZ = part.rotAX = part.rotAY = part.rotAZ = 0F;
                part.children.clear();
                CaptureInfo.ModelPart modelPart = new CaptureInfo.ModelPart(part);
                infos.add(new CaptureInfo(stack.last(), modelPart));
                modelToPart.put(renderer, modelPart);
            }
        }

        public ArrayList<CaptureInfo> combineTowards(PoseStack.Pose prevMid, PoseStack.Pose nextMid, ModelPartCapture other, float transitionProgress)
        {
            ArrayList<CaptureInfo> prevInfo = infos;
            ArrayList<CaptureInfo> nextInfo = other.infos;

            //Fill with empty parts first
            while(prevInfo.size() < nextInfo.size())
            {
                Project.Part part = new Project.Part(null, 0);
                part.boxes.clear();
                prevInfo.add(new CaptureInfo(prevMid, new CaptureInfo.ModelPart(part)));
            }

            while(nextInfo.size() < prevInfo.size())
            {
                Project.Part part = new Project.Part(null, 0);
                part.boxes.clear();
                nextInfo.add(new CaptureInfo(nextMid, new CaptureInfo.ModelPart(part)));
            }

            ArrayList<CaptureInfo> transitionInfos = new ArrayList<>();

            //sync up the box count
            for(int i = 0; i < prevInfo.size(); i++)
            {
                Project.Part oldPart = prevInfo.get(i).modelPart.part;
                Project.Part newPart = nextInfo.get(i).modelPart.part;

                ModelHelper.matchBoxesCount(oldPart, newPart);
                ModelHelper.matchBoxesCount(newPart, oldPart);

                transitionInfos.add(new CaptureInfo(RenderHelper.createInterimStackEntry(prevInfo.get(i).e, nextInfo.get(i).e, transitionProgress), new CaptureInfo.ModelPart(ModelHelper.createInterimPart(oldPart, newPart, transitionProgress))));
            }

            return transitionInfos;
        }

        public void render(PoseStack stack, MultiBufferSource buffer, int light, int overlay, float skinAlpha)
        {
            render(stack, buffer.getBuffer(RenderType.entityTranslucent(me.ichun.mods.morph.common.morph.MorphHandler.INSTANCE.getMorphSkinTexture())), light, overlay, 0F, 0F, 0F, skinAlpha);
        }

        public void render(PoseStack stack, MultiBufferSource buffer, int light, int overlay, float red, float green, float blue, float alpha)
        {
            render(stack, buffer.getBuffer(RenderType.entityTranslucent(me.ichun.mods.morph.common.morph.MorphHandler.INSTANCE.getMorphSkinTexture())), light, overlay, red, green, blue, alpha);
        }

        public void render(PoseStack stack, VertexConsumer vertexBuilder, int light, int overlay, float skinAlpha)
        {
            render(stack, vertexBuilder, light, overlay, 0F, 0F, 0F, skinAlpha);
        }

        public void render(PoseStack stack, VertexConsumer vertexBuilder, int light, int overlay, float red, float green, float blue, float alpha)
        {
            PoseStack newStack = stack != null ? stack : new PoseStack();
            int color = net.minecraft.util.ARGB.color((int)(alpha * 255F), (int)(red * 255F), (int)(green * 255F), (int)(blue * 255F));
            for(CaptureInfo info : infos)
            {
                newStack.pushPose();
                PoseStack.Pose entLast = newStack.last();
                PoseStack.Pose correctorLast = info.e;

                entLast.pose().mul(correctorLast.pose());
                entLast.normal().mul(correctorLast.normal());

                info.createAndRender(newStack, vertexBuilder, light, overlay, color);
                newStack.popPose();
            }
        }
    }

    public static class CaptureInfo
    {
        public final PoseStack.Pose e;
        public final CaptureInfo.ModelPart modelPart;

        public CaptureInfo(PoseStack.Pose e, CaptureInfo.ModelPart modelPart) {
            this.e = MorphRenderHandler.createPose(new org.joml.Matrix4f(e.pose()), new org.joml.Matrix3f(e.normal()));
            this.modelPart = modelPart;
        }

        public void createAndRender(PoseStack stack, VertexConsumer buffer, int light, int overlay, int color)
        {
            if(this.modelPart.model == null)
            {
                this.modelPart.model = ModelHelper.createModelPart(this.modelPart.part);
            }

            this.modelPart.model.render(stack, buffer, light, overlay, color);
        }

        private static class ModelPart
        {
            public final Project.Part part;
            public me.ichun.mods.ichunutil.client.model.TabulaModelRenderer model;

            private ModelPart(Project.Part part)
            {
                this.part = part;
            }
        }
    }
}
