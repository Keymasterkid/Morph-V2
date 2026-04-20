package me.ichun.mods.morph.client.render.hand;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
// Object /* InteractionHandInfo removed */ removed
// PlacementCorrector removed from API
import me.ichun.mods.ichunutil.client.model.util.ModelHelper;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.common.module.tabula.project.Project;
import me.ichun.mods.ichunutil.common.util.IOUtil;
import me.ichun.mods.morph.api.event.MorphLoadResourceEvent;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.resource.ResourceHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

@OnlyIn(Dist.CLIENT)
public final class HandHandler
{
    public interface HandInfo {
        me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[] getHandParts(net.minecraft.world.entity.HumanoidArm arm, net.minecraft.client.model.EntityModel<?> model);
        PoseStack[] getPlacementCorrectors(net.minecraft.world.entity.HumanoidArm arm);
        boolean setup();
        Class<?> getModelClass();
    }

    private static final HashMap<Class<?>, HandInfo> MODEL_HAND_INFO = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static HandHandler instance = new HandHandler();

    private MorphInfo lastMorphInfo;
    private float lastPartialTick;

    @SubscribeEvent
    public void onRenderInteractionHand(RenderHandEvent event) //if we're getting the event, the config has already assigned us;
    {
        Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if(mc.player != null && !mc.player.isRemoved()) //we need to cache this as the arm may be rendered even in the death screen.
        {
            lastMorphInfo = MorphHandler.INSTANCE.getMorphInfo(mc.player);
            lastPartialTick = event.getPartialTick();
        }
    }

    //Returns true if we have to override and render the InteractionHand.
    public boolean renderInteractionHand(PlayerRenderer playerRenderer, PoseStack stack, MultiBufferSource buffer, int light, LocalPlayer player, net.minecraft.client.model.geom.ModelPart arm, net.minecraft.client.model.geom.ModelPart armwear)
    {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.cameraEntity || MorphRenderHandler.isRenderingMorph) {
            return false;
        }
        if (player.isRemoved() || mc.level == null) {
            return false;
        }
        // NeoForge 1.21+: RenderHandEvent may run after arm hooks; refresh morph state here.
        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(player);
        if (info == null) {
            return false;
        }
        lastMorphInfo = info;
        lastPartialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        Morph.LOGGER.debug("[HandDebug] renderInteractionHand called. isMorphed={}, isRenderingMorph={}", info.isMorphed(), MorphRenderHandler.isRenderingMorph);

        float partialTick = lastPartialTick;
        float skinAlpha = info.getMorphSkinAlpha(partialTick);
        Morph.LOGGER.debug("[HandDebug] skinAlpha={}, isMorphed={}, morphProg={}", skinAlpha, info.isMorphed(), info.getMorphProgress(partialTick));
        if(skinAlpha > 0F || info.isMorphed()) // if we're supposed to override the InteractionHand render
        {
                me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[] handParts = null;
                PoseStack[] stacks = null;
                ResourceLocation texture = null;

                net.minecraft.world.entity.HumanoidArm humanoidArm = playerRenderer.getModel().rightArm != arm ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT; //default to right arm instead any mods override the player model

                float morphProg = info.getMorphProgress(partialTick);
                float transitionProg = info.getTransitionProgressSine(partialTick);
                if(morphProg < 1F && transitionProg < 1F) //still morphing, transition may be required.
                {
                    if(transitionProg <= 0F)
                    {
                        LivingEntity livingInstance = info.prevState.getEntityInstance(mc.player.level(), mc.player);
                        EntityRenderer<?, ?> entRenderer = mc.getEntityRenderDispatcher().getRenderer(livingInstance);
                        if(entRenderer instanceof LivingEntityRenderer<?, ?, ?>)
                        {
                            stack.pushPose();
                            stack.translate(0D, -500D, 0D);
                            MorphRenderHandler.renderLiving(entRenderer, livingInstance, stack, buffer, light, partialTick);
                            stack.popPose();

                            LivingEntityRenderer<LivingEntity, ?, ?> livingRenderer = (LivingEntityRenderer<LivingEntity, ?, ?>)entRenderer;
                            EntityModel<?> entityModel = livingRenderer.getModel();

                            HandInfo infoHelper = HandHandler.getHandInfo(entityModel.getClass());
                            if(infoHelper != null)
                            {
                                renderModelPreInteractionHandModelPartCopy(entityModel, livingInstance);
                                handParts = infoHelper.getHandParts(humanoidArm, entityModel);
                                stacks = infoHelper.getPlacementCorrectors(humanoidArm);
                                texture = getTextureLocation(entRenderer, livingInstance, partialTick);
                            }
                        }
                    }
                    else
                    {
                        me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[] prevHandParts = null;
                        PoseStack[] prevStacks = null;

                        me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[] nextHandParts = null;
                        PoseStack[] nextStacks = null;

                        LivingEntity prevInstance = info.prevState.getEntityInstance(mc.player.level(), mc.player);
                        EntityRenderer prevRenderer = mc.getEntityRenderDispatcher().getRenderer(prevInstance);

                        LivingEntity nextInstance = info.nextState.getEntityInstance(mc.player.level(), mc.player);
                        EntityRenderer nextRenderer = mc.getEntityRenderDispatcher().getRenderer(nextInstance);

                        stack.pushPose();
                        stack.translate(0D, -500D, 0D); //maybe I should just set scale to 0?
                        if(prevRenderer instanceof LivingEntityRenderer<?, ?, ?>)
                        {
                            MorphRenderHandler.renderLiving(prevRenderer, prevInstance, stack, buffer, light, partialTick);

                            LivingEntityRenderer<LivingEntity, ?, ?> livingRenderer = (LivingEntityRenderer<LivingEntity, ?, ?>)prevRenderer;
                            EntityModel<?> entityModel = livingRenderer.getModel();

                            HandInfo infoHelper = HandHandler.getHandInfo(entityModel.getClass());
                            if(infoHelper != null)
                            {
                                renderModelPreInteractionHandModelPartCopy(entityModel, prevInstance);

                                prevHandParts = infoHelper.getHandParts(humanoidArm, entityModel);
                                prevStacks = infoHelper.getPlacementCorrectors(humanoidArm);
                            }
                        }
                        if(nextRenderer instanceof LivingEntityRenderer<?, ?, ?>)
                        {
                            MorphRenderHandler.renderLiving(nextRenderer, nextInstance, stack, buffer, light, partialTick);

                            LivingEntityRenderer<LivingEntity, ?, ?> livingRenderer = (LivingEntityRenderer<LivingEntity, ?, ?>)nextRenderer;
                            EntityModel<?> entityModel = livingRenderer.getModel();

                            HandInfo infoHelper = HandHandler.getHandInfo(entityModel.getClass());
                            if(infoHelper != null)
                            {
                                renderModelPreInteractionHandModelPartCopy(entityModel, nextInstance);

                                nextHandParts = infoHelper.getHandParts(humanoidArm, entityModel);
                                nextStacks = infoHelper.getPlacementCorrectors(humanoidArm);
                            }
                        }
                        stack.popPose();

                        if(prevHandParts != null || nextHandParts != null)
                        {
                            if(prevHandParts == null)
                            {
                                prevHandParts = new me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[nextHandParts.length];
                                prevStacks = new PoseStack[nextHandParts.length];
                            }
                            if(nextHandParts == null)
                            {
                                nextHandParts = new me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[prevHandParts.length];
                                nextStacks = new PoseStack[prevHandParts.length];
                            }
                            if(prevHandParts.length < nextHandParts.length)
                            {
                                prevHandParts = Arrays.copyOf(prevHandParts, nextHandParts.length);
                                prevStacks = Arrays.copyOf(prevStacks, nextHandParts.length);
                            }
                            if(nextHandParts.length < prevHandParts.length)
                            {
                                nextHandParts = Arrays.copyOf(nextHandParts, prevHandParts.length);
                                nextStacks = Arrays.copyOf(nextStacks, prevHandParts.length);
                            }

                            //at this point the arrays have the same length
                            handParts = new me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[prevHandParts.length];
                            stacks = new PoseStack[prevHandParts.length];

                            for(int i = 0; i < handParts.length; i++)
                            {
                                Project.Part oldPart = ModelHelper.createPartFor(prevHandParts[i], true);
                                Project.Part newPart = ModelHelper.createPartFor(nextHandParts[i], true);

                                ModelHelper.matchBoxesCount(oldPart, newPart);
                                ModelHelper.matchBoxesCount(newPart, oldPart);

                                handParts[i] = (me.ichun.mods.ichunutil.client.model.TabulaModelRenderer)ModelHelper.createModelPart(ModelHelper.createInterimPart(oldPart, newPart, transitionProg), true);

                                if(prevStacks[i] != null || nextStacks[i] != null)
                                {
                                    PoseStack.Pose interimStackEntry = RenderHelper.createInterimStackEntry(prevStacks[i] != null ? prevStacks[i].last() : (new PoseStack()).last(), nextStacks[i] != null ? nextStacks[i].last() : (new PoseStack()).last(), transitionProg);
                                    PoseStack interimStack = new PoseStack();
                                    PoseStack.Pose last = interimStack.last();
                                    last.pose().set(interimStackEntry.pose());
                                    last.normal().set(interimStackEntry.normal());
                                    stacks[i] = interimStack;
                                }
                                else
                                {
                                    stacks[i] = null;
                                }
                            }
                        }
                    }
                }
                else //morph completed, just use nextState's entity instance
                {
                    LivingEntity livingInstance = info.isMorphed() ? info.nextState.getEntityInstance(mc.player.level(), mc.player) : mc.player;
                    EntityRenderer entRenderer = mc.getEntityRenderDispatcher().getRenderer(livingInstance);
                    if(entRenderer instanceof LivingEntityRenderer<?, ?, ?>)
                    {
                        stack.pushPose();
                        stack.translate(0D, -500D, 0D);
                        MorphRenderHandler.renderLiving(entRenderer, livingInstance, stack, buffer, light, partialTick);
                        stack.popPose();

                        LivingEntityRenderer<LivingEntity, ?, ?> livingRenderer = (LivingEntityRenderer<LivingEntity, ?, ?>)entRenderer;
                        EntityModel<?> entityModel = livingRenderer.getModel();

                        HandInfo infoHelper = HandHandler.getHandInfo(entityModel.getClass());
                        Morph.LOGGER.debug("[HandDebug] entityModel={}, infoHelper={}", entityModel.getClass().getSimpleName(), infoHelper != null);
                        if(infoHelper != null)
                        {
                            renderModelPreInteractionHandModelPartCopy(entityModel, livingInstance);

                            handParts = infoHelper.getHandParts(humanoidArm, entityModel);
                            stacks = infoHelper.getPlacementCorrectors(humanoidArm);
                            texture = getTextureLocation(entRenderer, livingInstance, partialTick);
                            Morph.LOGGER.debug("[HandDebug] handParts={}, stacks={}, texture={}", handParts != null ? handParts.length : "null", stacks != null ? stacks.length : "null", texture);
                        }
                    }

                    if(entRenderer instanceof PlayerRenderer && livingInstance instanceof LocalPlayer)//this must be a player
                    {
                        MorphRenderHandler.isRenderingMorph = true;
                        PlayerRenderer morphPlayerRenderer = (PlayerRenderer)entRenderer;
                        net.minecraft.client.player.AbstractClientPlayer acp = (net.minecraft.client.player.AbstractClientPlayer)livingInstance;
                        if(humanoidArm == net.minecraft.world.entity.HumanoidArm.LEFT)
                        {
                            morphPlayerRenderer.renderLeftHand(stack, buffer, light, acp.getSkin().texture(), acp.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM);
                        }
                        else
                        {
                            morphPlayerRenderer.renderRightHand(stack, buffer, light, acp.getSkin().texture(), acp.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM);
                        }
                        MorphRenderHandler.isRenderingMorph = false;

                        if(handParts != null && skinAlpha > 0F) //let's check the handParts just in case BipedModel.json is missing
                        {
                            renderModelPartsWithTexture(handParts, stacks, stack, buffer.getBuffer(RenderType.entityTranslucent(MorphHandler.INSTANCE.getMorphSkinTexture())), light, skinAlpha);
                        }
                        return true; //we're done here, the player render does the work for us
                    }
                }

                if(handParts != null)
                {
                    if(texture != null)
                    {
                        renderModelPartsWithTexture(handParts, stacks, stack, buffer.getBuffer(RenderType.entityTranslucent(texture)), light, 1F);
                    }

                    if(skinAlpha > 0F)
                    {
                        renderModelPartsWithTexture(handParts, stacks, stack, buffer.getBuffer(RenderType.entityTranslucent(MorphHandler.INSTANCE.getMorphSkinTexture())), light, skinAlpha);
                    }
                }
                return true;
        }

        return false;
    }

    private static ResourceLocation getTextureLocation(EntityRenderer renderer, LivingEntity entity, float partialTicks)
    {
        if(renderer instanceof LivingEntityRenderer)
        {
            LivingEntityRenderer<LivingEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, ?> typedRenderer = (LivingEntityRenderer<LivingEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, ?>)renderer;
            net.minecraft.client.renderer.entity.state.LivingEntityRenderState state = typedRenderer.createRenderState();
            typedRenderer.extractRenderState(entity, state, partialTicks);
            return typedRenderer.getTextureLocation(state);
        }
        return null;
    }

    private static void renderModelPreInteractionHandModelPartCopy(EntityModel<?> entityModel, LivingEntity livingInstance)
    {
        //these taken from PlayerRenderer - setupAnim sets model pose
        // entityModel.setupAnim(livingInstance, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F); // setupAnim might have changed signature
    }

    private static void renderModelPartsWithTexture(me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[] parts, PoseStack[] stacks, PoseStack stack, VertexConsumer buffer, int light, float alpha)
    {
        int color = net.minecraft.util.ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);
        for(int i = 0; i < parts.length; i++)
        {
            me.ichun.mods.ichunutil.client.model.TabulaModelRenderer part = parts[i];
            if(part == null)
            {
                continue;
            }

            float prevX = part.rotateAngleX;
            part.rotateAngleX = 0F;

            //taken from ModelPart.render
            if(part.showModel && !part.childModels.isEmpty())
            {
                stack.pushPose();

                part.translateRotate(stack);

                if(stacks[i] != null) //inject our stack to reverse rotation and do the appropriate translates
                {
                    stack.last().pose().mul(stacks[i].last().pose());
                    stack.last().normal().mul(stacks[i].last().normal());
                }

                part.doRender(stack.last(), buffer, light, OverlayTexture.NO_OVERLAY, color);

                for(me.ichun.mods.ichunutil.client.model.TabulaModelRenderer modelrenderer : part.childModels) {
                    modelrenderer.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, color);
                }

                stack.popPose();
            }

            part.rotateAngleX = prevX;
        }
    }

    private static boolean registered = false;
    public static void setState(boolean allowed)
    {
        if(allowed && !registered)
        {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(instance);
            registered = true;
            Morph.LOGGER.debug("[HandDebug] HandHandler registered on EVENT_BUS");
        }
        else if (!allowed && registered)
        {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(instance);
            registered = false;
        }
    }

    private static HandInfo getHandInfo(Class<?> clz)
    {
        if(clz == net.minecraft.client.model.EntityModel.class || clz == Object.class || clz == null)
        {
            return null;
        }
        if(MODEL_HAND_INFO.containsKey(clz))
        {
            return MODEL_HAND_INFO.get(clz);
        }
        HandInfo helper = getHandInfo(clz.getSuperclass());
        if(helper == null)
        {
            // Fallback to dynamic limb scanner
            HandInfoImpl dynamicFallback = new HandInfoImpl();
            // Just let it return this fallback, setup relies on predefined class names which we don't need for pure dynamic scan
            helper = dynamicFallback;
        }
        MODEL_HAND_INFO.put(clz, helper);
        return helper;
    }

    public static void loadHandInfos()
    {
        MODEL_HAND_INFO.clear();

        ArrayList<HandInfo> infos = new ArrayList<>();
        AtomicInteger skippedHandJson = new AtomicInteger(0);
        try
        {
            IOUtil.scourDirectoryForFiles(ResourceHandler.getMorphDir().resolve("hand"), p -> {
                if(p.getFileName().toString().endsWith(".json"))
                {
                    try
                    {
                        // JSON loading is complex with interfaces, we'll assume a concrete impl or map for now
                        // For a build fix, we'll keep the structure but fix the corrupted renames
                        HandInfo info = GSON.fromJson(FileUtils.readFileToString(p.toFile(), "UTF-8"), HandInfoImpl.class);
                        if(info.setup())
                        {
                            infos.add(info);
                            return true;
                        }
                        skippedHandJson.incrementAndGet();
                        Morph.LOGGER.debug("Skipped Hand Info JSON (model class not loaded): {}", p);
                    }
                    catch(IOException | JsonSyntaxException | IllegalStateException e)
                    {
                        Morph.LOGGER.error("Error reading file: {}", p);
                        e.printStackTrace();
                    }
                    return false;
                }
                return false;
            });
        }
        catch(IOException e)
        {
            Morph.LOGGER.error("Error reading Hand Infos");
            e.printStackTrace();
        }
        for(HandInfo info : infos)
        {
            if(MODEL_HAND_INFO.containsKey(info.getModelClass()))
            {
                Morph.LOGGER.warn("Hand Info for {} already exists!", info.getModelClass());
            }
            MODEL_HAND_INFO.put(info.getModelClass(), info);
        }

        int skipped = skippedHandJson.get();
        if(skipped > 0)
        {
            Morph.LOGGER.info("Skipped {} Hand Info JSON file(s) (forClass not on classpath)", skipped);
        }

        Morph.LOGGER.info("Loaded {} Hand Info(s)", MODEL_HAND_INFO.size());

        setState(true); // Failsafe activation
        
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new MorphLoadResourceEvent(MorphLoadResourceEvent.Type.InteractionHand));
    }
}
