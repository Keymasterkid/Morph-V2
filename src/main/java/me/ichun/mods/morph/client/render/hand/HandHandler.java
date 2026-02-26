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

@OnlyIn(Dist.CLIENT)
public final class HandHandler
{
    public interface HandInfo {
        me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[] getHandParts(net.minecraft.world.entity.HumanoidArm arm, net.minecraft.client.model.EntityModel model);
        PoseStack[] getPlacementCorrectors(net.minecraft.world.entity.HumanoidArm arm);
        boolean setup();
        Class<? extends net.minecraft.client.model.EntityModel> getModelClass();
    }

    private static final HashMap<Class<? extends net.minecraft.client.model.EntityModel>, HandInfo> MODEL_HAND_INFO = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static HandHandler instance = new HandHandler();

    private MorphInfo lastMorphInfo;
    private float lastPartialTick;

    @SubscribeEvent
    public void onRenderInteractionHand(RenderHandEvent event) //if we're getting the event, the config has already assigned us;
    {
        Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if(!mc.player.isRemoved()) //we need to cache this as the arm may be rendered even in the death screen.
        {
            lastMorphInfo = MorphHandler.INSTANCE.getMorphInfo(mc.player);
            lastPartialTick = event.getPartialTick();
        }
    }

    //Returns true if we have to override and render the InteractionHand.
    public boolean renderInteractionHand(PlayerRenderer playerRenderer, PoseStack stack, MultiBufferSource buffer, int light, LocalPlayer player, net.minecraft.client.model.geom.ModelPart arm, net.minecraft.client.model.geom.ModelPart armwear)
    {
        //Check if this is the player, and we have the player's morph info.
        if(player == net.minecraft.client.Minecraft.getInstance().cameraEntity && lastMorphInfo != null && !MorphRenderHandler.isRenderingMorph)
        {
            MorphInfo info = lastMorphInfo;
            float partialTick = lastPartialTick;
            float skinAlpha = info.getMorphSkinAlpha(partialTick);
            if(skinAlpha > 0F || info.isMorphed()) // if we're supposed to override the InteractionHand render
            {
                Minecraft mc = net.minecraft.client.Minecraft.getInstance();

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
                        EntityRenderer entRenderer = mc.getEntityRenderDispatcher().getRenderer(livingInstance);
                        if(entRenderer instanceof LivingEntityRenderer)
                        {
                            stack.pushPose();
                            stack.translate(0D, -500D, 0D);
                            MorphRenderHandler.renderLiving(entRenderer, livingInstance, stack, buffer, light, partialTick);
                            stack.popPose();

                            LivingEntityRenderer livingRenderer = (LivingEntityRenderer)entRenderer;
                            EntityModel entityModel = livingRenderer.getModel();

                            HandInfo infoHelper = HandHandler.getHandInfo(entityModel.getClass());
                            if(infoHelper != null)
                            {
                                renderModelPreInteractionHandModelPartCopy(entityModel, livingInstance);
                                handParts = infoHelper.getHandParts(humanoidArm, entityModel);
                                stacks = infoHelper.getPlacementCorrectors(humanoidArm);
                                texture = entRenderer.getTextureLocation(livingInstance);
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
                        if(prevRenderer instanceof LivingEntityRenderer)
                        {
                            MorphRenderHandler.renderLiving(prevRenderer, prevInstance, stack, buffer, light, partialTick);

                            LivingEntityRenderer livingRenderer = (LivingEntityRenderer)prevRenderer;
                            EntityModel entityModel = livingRenderer.getModel();

                            HandInfo infoHelper = HandHandler.getHandInfo(entityModel.getClass());
                            if(infoHelper != null)
                            {
                                renderModelPreInteractionHandModelPartCopy(entityModel, prevInstance);

                                prevHandParts = infoHelper.getHandParts(humanoidArm, entityModel);
                                prevStacks = infoHelper.getPlacementCorrectors(humanoidArm);
                            }
                        }
                        if(nextRenderer instanceof LivingEntityRenderer)
                        {
                            MorphRenderHandler.renderLiving(nextRenderer, nextInstance, stack, buffer, light, partialTick);

                            LivingEntityRenderer livingRenderer = (LivingEntityRenderer)nextRenderer;
                            EntityModel entityModel = livingRenderer.getModel();

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

                                // matchBoxAndChildrenCount stubbed for 1.21.1 build

                                handParts[i] = (me.ichun.mods.ichunutil.client.model.TabulaModelRenderer)ModelHelper.createModelPart(ModelHelper.createInterimPart(oldPart, newPart, transitionProg), true);

                                if(prevStacks[i] != null || nextStacks[i] != null)
                                {
                                    PoseStack.Pose interimStackEntry = RenderHelper.createInterimStackEntry(prevStacks[i] != null ? prevStacks[i].last() : (new PoseStack()).last(), nextStacks[i] != null ? nextStacks[i].last() : (new PoseStack()).last(), transitionProg);
                                    PoseStack interimStack = new PoseStack();
                                    PoseStack.Pose last = interimStack.last();
                                    last.pose().mul(interimStackEntry.pose());
                                    last.normal().mul(interimStackEntry.normal());
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
                    if(entRenderer instanceof LivingEntityRenderer)
                    {
                        stack.pushPose();
                        stack.translate(0D, -500D, 0D);
                        MorphRenderHandler.renderLiving(entRenderer, livingInstance, stack, buffer, light, partialTick);
                        stack.popPose();

                        LivingEntityRenderer livingRenderer = (LivingEntityRenderer)entRenderer;
                        EntityModel entityModel = livingRenderer.getModel();

                        HandInfo infoHelper = HandHandler.getHandInfo(entityModel.getClass());
                        if(infoHelper != null)
                        {
                            renderModelPreInteractionHandModelPartCopy(entityModel, livingInstance);

                            handParts = infoHelper.getHandParts(humanoidArm, entityModel);
                            stacks = infoHelper.getPlacementCorrectors(humanoidArm);
                            texture = entRenderer.getTextureLocation(livingInstance);
                        }
                    }

                    if(entRenderer instanceof PlayerRenderer && livingInstance instanceof LocalPlayer)//this must be a player
                    {
                        MorphRenderHandler.isRenderingMorph = true;
                        PlayerRenderer morphPlayerRenderer = (PlayerRenderer)entRenderer;
                        if(humanoidArm == net.minecraft.world.entity.HumanoidArm.LEFT)
                        {
                            morphPlayerRenderer.renderLeftHand(stack, buffer, light, (net.minecraft.client.player.AbstractClientPlayer)livingInstance);
                        }
                        else
                        {
                            morphPlayerRenderer.renderRightHand(stack, buffer, light, (net.minecraft.client.player.AbstractClientPlayer)livingInstance);
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
        }

        return false;
    }

    private static void renderModelPreInteractionHandModelPartCopy(EntityModel entityModel, LivingEntity livingInstance)
    {
        //these taken from PlayerRenderer - setupAnim sets model pose
        entityModel.setupAnim(livingInstance, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static void renderModelPartsWithTexture(me.ichun.mods.ichunutil.client.model.TabulaModelRenderer[] parts, PoseStack[] stacks, PoseStack stack, VertexConsumer buffer, int light, float alpha)
    {
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

                part.doRender(stack.last(), buffer, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, alpha);

                for(me.ichun.mods.ichunutil.client.model.TabulaModelRenderer modelrenderer : part.childModels) {
                    modelrenderer.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, alpha);
                }

                stack.popPose();
            }

            part.rotateAngleX = prevX;
        }
    }

    public static void setState(boolean allowed)
    {
        // setState is a no-op stub — HandHandler registration is managed elsewhere
    }

    @Nullable
    private static HandInfo getHandInfo(Class<? extends net.minecraft.client.model.EntityModel> clz)
    {
        if(MODEL_HAND_INFO.containsKey(clz))
        {
            return MODEL_HAND_INFO.get(clz);
        }
        HandInfo helper = null;
        Class clzz = clz.getSuperclass();
        if(clzz != EntityModel.class)
        {
            helper = getHandInfo(clzz);
        }
        MODEL_HAND_INFO.put(clz, helper);
        return helper;
    }

    public static void loadHandInfos()
    {
        MODEL_HAND_INFO.clear();

        ArrayList<HandInfo> infos = new ArrayList<>();
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

        Morph.LOGGER.info("Loaded {} Hand Info(s)", MODEL_HAND_INFO.size());

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new MorphLoadResourceEvent(MorphLoadResourceEvent.Type.InteractionHand));
    }
}
