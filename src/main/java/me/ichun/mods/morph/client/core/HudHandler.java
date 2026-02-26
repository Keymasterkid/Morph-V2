package me.ichun.mods.morph.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.mouse.MouseHelper;
import me.ichun.mods.ichunutil.client.key.KeyBind;
import me.ichun.mods.ichunutil.client.render.NativeImageTexture;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.ichunutil.common.iChunUtil;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.client.gui.biomass.WorkspaceMorph;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.biomass.Upgrades;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import me.ichun.mods.morph.common.packet.PacketMorphInput;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.GraphicsStatus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
// redundant static import
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
// InputEvent directly imported not needed - use FQN
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Mob;
import com.mojang.blaze3d.vertex.VertexConsumer;

@OnlyIn(Dist.CLIENT)
public class HudHandler
{
    public static final ResourceLocation TEX_QS_FAVOURITE = ResourceLocation.fromNamespaceAndPath("morph", "textures/gui/fav.png");
    public static final ResourceLocation TEX_QS_SELECTED = ResourceLocation.fromNamespaceAndPath("morph", "textures/gui/gui_selected.png");
    public static final ResourceLocation TEX_QS_UNSELECTED = ResourceLocation.fromNamespaceAndPath("morph", "textures/gui/gui_unselected.png");
    public static final ResourceLocation TEX_QS_UNSELECTED_SIDE = ResourceLocation.fromNamespaceAndPath("morph", "textures/gui/gui_unselected_side.png");

    private static final PoseStack LIGHT_STACK = Util.make(new PoseStack(), stack -> stack.translate(1D, -1D, 0D));

    private final Minecraft mc;

    //selector stuff
    private static final int SHOW_SELECTOR_TIME = 8;
    private static final int INDEX_TIME = 4;

    public boolean showSelector = false;
    public int showTime = 0;

    public int indexChangeTime = 0;
    public double lastIndexVert = 0D;
    public double lastIndexHori = 0D;

    public int indexVert = 0;
    public int indexHori = 0;

    //radial stuff
    private static final int RADIAL_TIME = 3;

    public boolean showRadial = false;
    public int radialTime = 0;

    public RadialMode radialMode = null;

    public ArrayList<MorphVariant> radialFavourites = null;

    //biomass bar stuff
    private static final int BAR_TIME = 8;
    private static NativeImageTexture barAbstractTexture = null;
    private static boolean barTextureGenerated = false;

    public boolean barRequiresReset;

    public int barShowTime = 0;

    private BiomassValue barCapacity;
    private BiomassValue barCriticalCapacity;
    private BiomassValue barCurrentBiomass;
    private double barAbilityCost = 0D;

    private int barInsufficientFlash;

    //key listeners
    public boolean keyEscDown;
    public boolean keyEnterDown;
    public boolean keyDeleteDown;

    public boolean keyDirUp;
    public boolean keyDirDown;
    public boolean keyDirLeft;
    public boolean keyDirRight;

    public HashMap<MorphVariant, MorphState> morphStates = new HashMap<>();

    public HudHandler(Minecraft mc, PlayerMorphData morphData)
    {
        this.mc = mc;

        barCapacity = new BiomassValue(morphData.getBiomassUpgradeValue(Upgrades.ID_BIOMASS_CAPACITY));
        barCriticalCapacity = new BiomassValue(morphData.getBiomassUpgradeValue(Upgrades.ID_BIOMASS_CRITICAL_CAPACITY));
        barCurrentBiomass = new BiomassValue(morphData.biomass);
    }

    public void handleInput(KeyBind keyBind, boolean isReleased)
    {
        if(mc.player == null) // ???what
        {
            return;
        }

        if(keyBind == KeyBinds.keySelectorUp || keyBind == KeyBinds.keySelectorDown || keyBind == KeyBinds.keySelectorLeft || keyBind == KeyBinds.keySelectorRight || keyBind == KeyBinds.keyFavourite)
        {
            handleMorphInput(keyBind, isReleased);
        }
        //TODO disabled because too many people asking stupid questions
//        else if(keyBind == KeyBinds.keyBiomass)
//        {
//            if(MorphHandler.INSTANCE.hasUnlockedBiomass(mc.player))
//            {
//                mc.setScreen(new WorkspaceMorph(mc.screen));
//            }
//            else if(MorphHandler.INSTANCE.getMorphModeName().equals("default"))
//            {
//                barInsufficientFlash = 20;
//            }
//        }
    }

    private void handleMorphInput(KeyBind keyBind, boolean isReleased)
    {
        if(MorphHandler.INSTANCE.canShowMorphSelector(mc.player))
        {
            if(keyBind == KeyBinds.keySelectorDown || keyBind == KeyBinds.keySelectorUp || keyBind == KeyBinds.keySelectorLeft || keyBind == KeyBinds.keySelectorRight)
            {
                if(showSelector)
                {
                    shiftIndexSelector(keyBind == KeyBinds.keySelectorDown || keyBind == KeyBinds.keySelectorRight, keyBind == KeyBinds.keySelectorLeft || keyBind == KeyBinds.keySelectorRight);
                }
                else
                {
                    showSelector = true;

                    setIndicesToCurrentMorph();

                    //reset the keydowns
                    keyEscDown = false;
                    keyEnterDown = false;
                }
            }
            else if(keyBind == KeyBinds.keyFavourite)
            {
                if(showSelector)
                {
                    if(isReleased)
                    {
                        toggleFavourite();
                    }
                }
                else if(!isReleased)
                {
                    //open radial menu
                    if(!showRadial)
                    {
                        gatherFavourites();

                        showRadial = true;
                        radialTime = 0;
                        radialMode = RadialMode.FAVOURITE;

                        mc.mouseHandler.releaseMouse();
                    }
                }
                else if(radialMode == RadialMode.FAVOURITE)
                {
                    //confirm radial menu selection
                    confirmRadial();
                }
            }
        }
        else if(!(keyBind == KeyBinds.keyFavourite && isReleased)) //favourite triggers twice
        {
            barInsufficientFlash = 20;
        }
    }

    public void setIndicesToCurrentMorph()
    {
        PlayerMorphData morphData = getMorphData();

        indexVert = indexHori = 0; //the player default morph should always be first.

        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(mc.player);
        if(info.isMorphed())
        {
            MorphVariant currentMorph = info.nextState.variant;
            for(int i = 0; i < morphData.morphs.size(); i++)
            {
                MorphVariant variant = morphData.morphs.get(i);
                if(variant.id.equals(currentMorph.id))
                {
                    indexVert = i;

                    for(int i1 = 0; i1 < variant.variants.size(); i1++)
                    {
                        MorphVariant.Variant morphVariant = variant.variants.get(i1);
                        if(morphVariant.identifier.equals(currentMorph.thisVariant.identifier))
                        {
                            indexHori = i1;
                            lastIndexHori = variant.variants.size() - 1;
                            break;
                        }
                    }

                    break;
                }
            }
        }
        if(mc.player instanceof LocalPlayer)
        {
            LocalPlayer player = (LocalPlayer)mc.player;
        }

        lastIndexVert = indexVert;
        indexChangeTime = 0;
    }

    public void updateMorphs()
    {
        if(shouldRenderSelector())
        {
            validateIndices();
        }
    }

    private void tick()
    {
        //biomass bar stuff
        if(shouldShowBiomassBar())
        {
            barShowTime++;
            if(barShowTime > BAR_TIME)
            {
                barShowTime = BAR_TIME;

                updateBiomassBar();
            }
        }
        else
        {
            barShowTime--;
            if(barShowTime < 0)
            {
                barShowTime = 0;
            }
        }


        //selector stuff
        if(showSelector)
        {
            showTime++;
            if(showTime > SHOW_SELECTOR_TIME)
            {
                showTime = SHOW_SELECTOR_TIME;
            }
        }
        else
        {
            showTime--;
            if(showTime < 0)
            {
                showTime = 0;
            }
        }

        indexChangeTime++;
        if(indexChangeTime > INDEX_TIME)
        {
            indexChangeTime = INDEX_TIME;
            lastIndexVert = indexVert;
            lastIndexHori = indexHori;
        }

        //radial stuff
        if(showRadial)
        {
            radialTime++;
            if(radialTime > RADIAL_TIME)
            {
                radialTime = RADIAL_TIME;
            }

            //TODO can radial menus show during selector? (later edit: yes, to select abilities)
        }

        updateKeyListeners();
    }

    private void updateKeyListeners()
    {
        long handle = mc.getWindow().getWindow();
        boolean isEnterDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, com.mojang.blaze3d.platform.InputConstants.KEY_RETURN) || com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_KP_ENTER);
        boolean isEscDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
        boolean isDeleteDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, com.mojang.blaze3d.platform.InputConstants.KEY_DELETE) || com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_KP_DECIMAL);

        boolean isDirUp = com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, com.mojang.blaze3d.platform.InputConstants.KEY_UP);
        boolean isDirDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, com.mojang.blaze3d.platform.InputConstants.KEY_DOWN);
        boolean isDirLeft = com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, com.mojang.blaze3d.platform.InputConstants.KEY_LEFT);
        boolean isDirRight = com.mojang.blaze3d.platform.InputConstants.isKeyDown(handle, com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT);

        if(showSelector || showRadial)
        {
            if(!keyEnterDown && isEnterDown)
            {
                if(showSelector)
                {
                    confirmSelector();
                }
                else
                {
                    confirmRadial();
                }
            }

            if(mc.screen != null || !keyEscDown && isEscDown)
            {
                if(showSelector)
                {
                    closeSelector();
                }
                else
                {
                    closeRadial();
                }
            }

            if(showSelector) //Only selector
            {
                if(!keyDeleteDown && isDeleteDown)
                {
                    deleteSelector();
                }

                if(!keyDirUp && isDirUp)
                {
                    shiftIndexSelector(false, false);
                }

                if(!keyDirDown && isDirDown)
                {
                    shiftIndexSelector(true, false);
                }

                if(!keyDirLeft && isDirLeft)
                {
                    shiftIndexSelector(false, true);
                }

                if(!keyDirRight && isDirRight)
                {
                    shiftIndexSelector(true, true);
                }
            }
        }
        keyEnterDown = isEnterDown;
        keyEscDown = isEscDown;
        keyDeleteDown = isDeleteDown;

        keyDirUp = isDirUp;
        keyDirDown = isDirDown;
        keyDirLeft = isDirLeft;
        keyDirRight = isDirRight;
    }

    private void updateBiomassBar()
    {
        //if we have to flash the bar update the timer
        if(barInsufficientFlash > 0)
        {
            barInsufficientFlash--;
        }

        //Update the bar info
        barCapacity.tick();
        barCriticalCapacity.tick();
        barCurrentBiomass.tick();
    }

    private void confirmSelector()
    {
        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(mc.player);
        MorphVariant.Variant variant = getMorphData().morphs.get(indexVert).variants.get(indexHori);

        if(!info.isCurrentlyThisVariant(variant)) //if we're already morphed to this, don't morph to this.
        {
            Morph.channel.sendToServer(new PacketMorphInput(variant.identifier, false, false, false));
        }

        closeSelector();
    }

    private void deleteSelector()
    {
        if(MorphHandler.INSTANCE.getMorphModeName().equals("classic"))
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(mc.player);
            MorphVariant.Variant variant = getMorphData().morphs.get(indexVert).variants.get(indexHori);

            if(!info.isCurrentlyThisVariant(variant) && !variant.identifier.equals(MorphVariant.IDENTIFIER_DEFAULT_PLAYER_STATE)) //if we're already morphed to this, don't delete it. Also don't delete our default morph
            {
                Morph.channel.sendToServer(new PacketMorphInput(variant.identifier, false, false, true));
            }
        }
    }

    private void closeSelector()
    {
        showSelector = false;

        if(mc.screen instanceof PauseScreen)
        {
            mc.setScreen(null);
        }

        //makes the horizontal slider slide back in
        PlayerMorphData morphData = getMorphData();
        indexHori = morphData.morphs.get(indexVert).variants.size() - 1;
        indexChangeTime = 0;
    }

    private void confirmRadial()
    {
        if(isMouseOutsideRadialDeadZone(mc.getWindow()))
        {
            if(radialMode == RadialMode.FAVOURITE)
            {
                //morph to the selected Morph
                MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(mc.player);
                MorphVariant variant = radialFavourites.get(MouseHelper.getSelectedIndex(radialFavourites.size()));

                if(!info.isCurrentlyThisVariant(variant.thisVariant)) //if we're already morphed to this, don't morph to this.
                {
                    Morph.channel.sendToServer(new PacketMorphInput(variant.thisVariant.identifier, false, false, false));
                }

                radialFavourites = null; //enjoy, GC.
            }
        }
        closeRadial();
    }

    private void closeRadial()
    {
        showRadial = false;
        radialMode = null;

        if(mc.screen instanceof PauseScreen)
        {
            mc.setScreen(null);
        }

        mc.mouseHandler.grabMouse();
    }

    private void toggleFavourite()
    {
        MorphVariant.Variant variant = getMorphData().morphs.get(indexVert).variants.get(indexHori);
        if(!variant.identifier.equals(MorphVariant.IDENTIFIER_DEFAULT_PLAYER_STATE)) //you can't favourite your personal variant
        {
            variant.isFavourite = !variant.isFavourite;

            Morph.channel.sendToServer(new PacketMorphInput(variant.identifier, true, variant.isFavourite, false));
        }
    }

    private void gatherFavourites()
    {
        radialFavourites = new ArrayList<>();
        radialFavourites.add(MorphVariant.createPlayerMorph(mc.player.getGameProfile().getId(), true));
        radialFavourites.get(0).thisVariant.identifier = MorphVariant.IDENTIFIER_DEFAULT_PLAYER_STATE;

        PlayerMorphData morphData = getMorphData();
        for(MorphVariant morph : morphData.morphs)
        {
            if(morph.hasFavourite())
            {
                for(MorphVariant.Variant variant : morph.variants)
                {
                    if(variant.isFavourite)
                    {
                        radialFavourites.add(morph.getAsVariant(variant));
                    }
                }
            }
        }
    }

    private void shiftIndexSelector(boolean isDown, boolean isHori)
    {
        PlayerMorphData morphData = getMorphData();
        if(isDown)
        {
            if(isHori) //adjust horizontally
            {
                lastIndexHori = (lastIndexHori + (indexHori - lastIndexHori) * (EntityHelper.sineifyProgress(Mth.clamp((float)indexChangeTime / INDEX_TIME, 0F, 1F))));

                indexHori++;
                if(indexHori >= morphData.morphs.get(indexVert).variants.size())
                {
                    indexHori = 0;
                }
            }
            else
            {
                lastIndexVert = (lastIndexVert + (indexVert - lastIndexVert) * (EntityHelper.sineifyProgress(Mth.clamp((float)indexChangeTime / INDEX_TIME, 0F, 1F))));

                indexVert++;
                if(indexVert >= morphData.morphs.size())
                {
                    indexVert = 0;
                }

                if(morphData.morphs.size() > 1)
                {
                    lastIndexHori = morphData.morphs.get(indexVert).variants.size() - 1;
                    indexHori = 0;//reset the hori index
                }
            }
        }
        else
        {
            if(isHori) //adjust horizontally
            {
                lastIndexHori = (lastIndexHori + (indexHori - lastIndexHori) * (EntityHelper.sineifyProgress(Mth.clamp((float)indexChangeTime / INDEX_TIME, 0F, 1F))));

                indexHori--;
                if(indexHori < 0)
                {
                    indexHori = morphData.morphs.get(indexVert).variants.size() - 1;
                }
            }
            else
            {
                lastIndexVert = (lastIndexVert + (indexVert - lastIndexVert) * (EntityHelper.sineifyProgress(Mth.clamp((float)indexChangeTime / INDEX_TIME, 0F, 1F))));

                indexVert--;
                if(indexVert < 0)
                {
                    indexVert = morphData.morphs.size() - 1;
                }

                if(morphData.morphs.size() > 1)
                {
                    lastIndexHori = morphData.morphs.get(indexVert).variants.size() - 1;
                    indexHori = 0;//reset the hori index
                }
            }
        }
        indexChangeTime = 0;
    }

    private void validateIndices()
    {
        PlayerMorphData morphData = getMorphData();
        if(indexVert >= morphData.morphs.size())
        {
            indexVert = 0;

            lastIndexHori = morphData.morphs.get(indexVert).variants.size() - 1;
            indexHori = 0;//reset the hori index
        }
        else if(indexVert < 0)
        {
            indexVert = morphData.morphs.size() - 1;

            lastIndexHori = morphData.morphs.get(indexVert).variants.size() - 1;
            indexHori = 0;//reset the hori index
        }

        if(indexHori >= morphData.morphs.get(indexVert).variants.size())
        {
            indexHori = 0;
        }
        else if(indexHori < 0)
        {
            indexHori = morphData.morphs.get(indexVert).variants.size() - 1;
        }

    }

    private void drawSelector(GuiGraphics guiGraphics, float partialTick, Window window)
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();

        double zLevel = 0D;

        double size = 50 * Morph.configClient.selectorScale;

        float outProg = EntityHelper.sineifyProgress(Mth.clamp((showSelector ? ((showTime + partialTick) / SHOW_SELECTOR_TIME) : (showTime - partialTick) / SHOW_SELECTOR_TIME), 0F, 1F));

        int top = Morph.configClient.selectorDistanceFromTop;

        double posX = -size * (1F - outProg);

        PlayerMorphData morphData = getMorphData();

        float indexChangeTimeProg = EntityHelper.sineifyProgress(Mth.clamp((indexChangeTime + partialTick) / INDEX_TIME, 0F, 1F));

        guiGraphics.pose().pushPose();

        //Draw the vertical stack
        double indexVertProg = (lastIndexVert + (indexVert - lastIndexVert) * indexChangeTimeProg);
        double unSelY = indexVertProg * size;
        double height = size * morphData.morphs.size();

        guiGraphics.blit(TEX_QS_UNSELECTED, (int)posX, (int)(top - unSelY), (int)size, (int)height, 0F, 0F, (int)size, (int)height, (int)size, 1);

        //Draw the horizontal stack
        double indexHoriProg = (lastIndexHori + (indexHori - lastIndexHori) * indexChangeTimeProg);
        double unSelX = indexHoriProg * size;
        double width = size * (morphData.morphs.get(indexVert).variants.size() - 1);

        if(width > 0)
        {
            guiGraphics.blit(TEX_QS_UNSELECTED_SIDE, (int)(posX - unSelX), top, (int)width, (int)size, 0F, 0F, (int)width, (int)size, (int)width, (int)size);
        }

        //Draw the end of the horizontal stack
        guiGraphics.blit(TEX_QS_UNSELECTED, (int)(posX - unSelX + width), top, (int)size, (int)size, 0F, 0F, (int)size, (int)size, (int)size, (int)size);

        //Draw the selected marker
        guiGraphics.blit(TEX_QS_SELECTED, (int)posX, top, (int)size, (int)size, 0F, 0F, (int)size, (int)size, (int)size, (int)size);

        //Draw the entities
        int screenHeight = window.getGuiScaledHeight();

        int firstMorphIndex = Math.max(0, indexVert - ((int)Math.ceil(top / size) + 1)); //first index to render, +1 because of the scrolling
        int lastMorphIndex = Math.min(morphData.morphs.size(), indexVert + ((int)Math.ceil((screenHeight - top) / size) + 1));

        Player player = mc.player;

        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(player);

        MorphVariant currentMorph;
        if(info.isMorphed())
        {
            currentMorph = info.nextState.variant;
        }
        else
        {
            currentMorph = MorphVariant.createPlayerMorph(player.getGameProfile().getId(), true);
            currentMorph.thisVariant.identifier = MorphVariant.IDENTIFIER_DEFAULT_PLAYER_STATE;
        }

        for(int i = firstMorphIndex; i < lastMorphIndex; i++)
        {
            MorphVariant morph = morphData.morphs.get(i);
            double indexSizeHeight = (i - indexVertProg) * size;
            double morphHeight = (top + size * 0.775D) + indexSizeHeight;
            double textHeight = (top + (size - mc.font.lineHeight) / 2) + indexSizeHeight;
            double favHeight = top + (size * 0.13D) + indexSizeHeight;
            if(i == indexVert) //is selected
            {
                for(int j = Math.max(0, indexHori - 1); j < morph.variants.size(); j++)
                {
                    double indexHeightWidth = (j - indexHoriProg) * size;
                    double morphBoxX = posX + indexHeightWidth;
                    MorphVariant.Variant theVariant = morph.variants.get(j);
                    MorphVariant variant = morph.getAsVariant(theVariant);
                    MorphState state = morphStates.computeIfAbsent(variant, v -> new MorphState(variant, player));
                    state.variant.thisVariant.isFavourite = theVariant.isFavourite;

                    LivingEntity living = state.getEntityInstance(player.level(), player);

                    if(morphBoxX < window.getGuiScaledWidth() + size) //Only render the entity and the favourite star if it's at most just barely off screen
                    {
                        EntityDimensions livingSize = living.getDimensions(net.minecraft.world.entity.Pose.STANDING);
                        float entSize = Math.max(livingSize.width(), livingSize.height()) / 1.95F; //1.95F = zombie height

                        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
                        LivingEntityRenderer livingRenderer = (LivingEntityRenderer)dispatcher.getRenderer(living);
                        // getRenderType is protected. Using a generic one for now.
                        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(me.ichun.mods.morph.common.morph.MorphHandler.INSTANCE.getMorphSkinTexture()));
                        int light = 15728880; // full bright
                        float forceDuringInvisibility = 1.0F; // always render fully opaque

                        // renderLiving(livingRenderer, living, stack, buffer, light, partialTick, forceDuringInvisibility);
                        if (living instanceof Mob && living.isBaby()) //Checked in EntityRenderDispatcher
                        {
                            // state.renderedShadowSize = livingRenderer.shadowRadius * 0.5F;
                        }
                        else
                        {
                            // state.renderedShadowSize = livingRenderer.shadowRadius;
                        }
                        if(j == indexHori) //if it is selected, prevent the downscale.
                        {
                            if(showSelector)
                            {
                                entSize *= (1F - indexChangeTimeProg);
                            }
                            else if(j == Math.round(lastIndexHori) && indexChangeTimeProg < 1F || variant.equals(currentMorph))
                            {
                                entSize = Math.max(entSize, 0.5F); //keep entity at least half sized (was 0F which made it invisible)
                            }
                        }

                float entScale = 0.45F * (1F / Math.max(1F, entSize)) * (float)size;

                        renderMorphEntity(guiGraphics, living, (posX + (size / 2D) - 2) + indexHeightWidth, morphHeight, zLevel + (j == indexHori ? 100F : 50F), entScale);

                        zLevel += 30F;

                        if(j == 0 && morph.hasFavourite() || state.variant.thisVariant.isFavourite)
                        {
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(0F, 0F, 300F);

                            if(!state.variant.thisVariant.isFavourite)
                            {
                                RenderSystem.setShaderColor(0F, 1F, 1F, 1F); // green star
                            }

                            guiGraphics.blit(TEX_QS_FAVOURITE, (int)(posX + 1 + indexHeightWidth), (int)favHeight, (int)(size * 0.15D), (int)(size * 0.15D), 0F, 0F, (int)size, (int)size, (int)size, (int)size);
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                            guiGraphics.pose().popPose();
                        }
                    }

                    //Render the name of the mob
                    if(j == morph.variants.size() - 1)
                    {
                        net.minecraft.network.chat.MutableComponent customName = null;
                        MorphVariant selectedVariant = morph.getAsVariant(morph.variants.get(indexHori));
                        MorphState selectedState = morphStates.computeIfAbsent(selectedVariant, v -> new MorphState(selectedVariant, player));

                        LivingEntity selectedLiving = selectedState.getEntityInstance(player.level(), player);

                        net.minecraft.network.chat.MutableComponent text;

                        EntityType<?> value = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(variant.id);
                        if(value != null)
                        {
                            if(!selectedLiving.getName().getString().equals(value.getDescription().getString()) && showTime >= SHOW_SELECTOR_TIME) //has a custom name
                            {
                                customName = selectedLiving.getName().copy();
                                customName.setStyle(customName.getStyle().withItalic(true));
                            }
                            text = Component.translatable(value.getDescriptionId());
                        }
                        else
                        {
                            text = Component.translatable("morph.morph.type.unknown");
                        }

                        if(showTime < SHOW_SELECTOR_TIME)
                        {
                            text.setStyle(text.getStyle().withColor(ChatFormatting.GOLD));
                        }
                        else
                        {
                            text.setStyle(text.getStyle().withColor(ChatFormatting.YELLOW));
                        }

                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(0F, 0F, 500F);
                        float textPosX = (float)((posX + size + 5) + indexHeightWidth);
                        if(textPosX > window.getGuiScaledWidth() - mc.font.width(text) - 2)
                        {
                            textPosX = window.getGuiScaledWidth() - mc.font.width(text) - 2;
                        }
                        guiGraphics.drawString(mc.font, text, (int)textPosX, (int)textHeight, 0xFFFFFF);

                        if(customName != null)
                        {
                            guiGraphics.drawString(mc.font, customName, 3, (int)(top + size - mc.font.lineHeight - 5 + indexSizeHeight), 0xFFFFFF);
                        }
                        guiGraphics.pose().popPose();
                    }
                }
            }
            else
            {
                MorphVariant.Variant theVariant = morph.variants.get(0);
                MorphVariant variant = morph.getAsVariant(theVariant);
                MorphState state = morphStates.computeIfAbsent(variant, v -> new MorphState(variant, player));
                state.variant.thisVariant.isFavourite = theVariant.isFavourite;

                LivingEntity living = state.getEntityInstance(player.level(), player);

                EntityDimensions livingSize = living.getDimensions(net.minecraft.world.entity.Pose.STANDING);

                float entSize = Math.max(livingSize.width(), livingSize.height()) / 1.95F; //1.95F = zombie height

                if(i == Math.round(lastIndexVert)) //last selected
                {
                    entSize *= indexChangeTimeProg;
                }

                float entScale = 0.45F * (1F / Math.max(1F, entSize)) * (float)size;

                renderMorphEntity(guiGraphics, living, (int)(posX + (size / 2D) - 2), morphHeight, zLevel, entScale);

                zLevel += 30F;

                net.minecraft.network.chat.MutableComponent text;

                EntityType<?> value = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(variant.id);
                if(value != null)
                {
                    text = Component.translatable(value.getDescriptionId());
                }
                else
                {
                    text = Component.translatable("morph.morph.type.unknown");
                }

                if(morph.id.equals(currentMorph.id) && morph.containsVariant(currentMorph))
                {
                    text.setStyle(text.getStyle().withColor(ChatFormatting.GOLD));
                }
                else
                {
                    text.setStyle(text.getStyle().withColor(ChatFormatting.WHITE));
                }

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0F, 0F, 300F);
                if(morph.hasFavourite())
                {
                    if(!state.variant.thisVariant.isFavourite)
                    {
                        RenderSystem.setShaderColor(0F, 1F, 1F, 1F);
                    }

                    guiGraphics.blit(TEX_QS_FAVOURITE, (int)posX + 1, (int)favHeight, (int)(size * 0.15D), (int)(size * 0.15D), 0F, 0F, (int)size, (int)size, (int)size, (int)size);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }

                guiGraphics.drawString(mc.font, text, (int)(posX + size + 5), (int)textHeight, 0xFFFFFF);
                guiGraphics.pose().popPose();
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        guiGraphics.pose().popPose();
    }

    private void drawRadial(GuiGraphics guiGraphics, float partialTick, Window window)
    {
        /*
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();

        double diameter = Math.min(window.getGuiScaledWidth(), window.getGuiScaledHeight()) * Morph.configClient.radialScale;
        double radius = diameter / 2D;

        double radialProg = EntityHelper.sineifyProgress(Mth.clamp((radialTime + partialTick) / RADIAL_TIME, 0F, 1F));
        double radialDist = radius * radialProg;
        float textScale = (float)radius / 96.375F;
        float deadzoneScale = 0.55F;
        float deadzoneSize = (float)radius * deadzoneScale;

        double distanceFromDeadzone = MouseHelper.getMouseDistanceFromCenter(window) - deadzoneSize;

        float bonusScale = Mth.clamp((float)(distanceFromDeadzone / (radius * (1F - deadzoneScale) * 0.5F)), 0F, 1F);

        double centerX = window.getGuiScaledWidth() / 2D;
        double centerY = window.getGuiScaledHeight() / 2D;

        float zLevel = 0F;

        int slices = mc.options.graphicFanciness == GraphicsStatus.FAST ? 30 : 100;

        stack.pushPose();
        stack.translate(centerX, centerY, zLevel);
        Matrix4f matrix = stack.last().pose();
        RenderSystem.disableTexture();
        com.mojang.blaze3d.vertex.Tesselator tessellator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        bufferbuilder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for(int i = 0; i <= slices; i++)
        {
            double angle = Math.PI * 2 * i / slices;
        }
        */
    }

    private boolean bindBiomassBarTexture()
    {
        /*
        if(barAbstractTexture == null && !barTextureGenerated)
        {
            barTextureGenerated = true;

            //Copied from SimpleTexture
            try(SimpleTexture.TextureData textureData = SimpleTexture.TextureData.getTextureData(mc.getResourceManager(), GuiGraphics.GUI_ICONS_LOCATION))
            {
                textureData.checkException();

                NativeImage image = textureData.getNativeImage();

                for(int x = 0; x < image.getWidth(); x++)
                {
                    for(int y = 0; y < image.getHeight(); y++)
                    {
                        int clr = image.getPixelRGBA(x, y); //Actually ARGB
                        if((clr >> 24 & 0xff) > 0) //not invisible
                        {
                            float[] hsb = Color.RGBtoHSB(clr >> 16 & 0xff, clr >> 8 & 0xff, clr & 0xff, null);
                            hsb[1] = 0F; //set the saturation to 0
                            image.setPixelRGBA(x, y, ((clr >> 24 & 0xff) << 24) | (Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xffffff));
                        }
                    }
                }

                barAbstractTexture = new NativeImageTexture(image);

                mc.getTextureManager().loadTexture(barTexture.getResourceLocation(), barTexture);
            }
            catch(IOException e)
            {
                Morph.LOGGER.error("Error creating Icon texture data!");
                e.printStackTrace();
            }
        }

        if(barAbstractTexture != null)
        {
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, barTexture.getResourceLocation());

            return true;
        }
        */
        return false;
    }

    private boolean isMouseOutsideRadialDeadZone(Window window)
    {
        double diameter = Math.min(window.getGuiScaledWidth(), window.getGuiScaledHeight()) * Morph.configClient.radialScale;
        double deadZoneBorder = diameter / 2D * 0.55F;

        return MouseHelper.getMouseDistanceFromCenter(window) > deadZoneBorder;
    }

    private boolean shouldRenderSelector()
    {
        return showSelector || showTime > 0;
    }

    private boolean shouldShowBiomassBar()
    {
        return Morph.configClient.biomassBarMode == 1 && biomassBarRequiresUpdate() || Morph.configClient.biomassBarMode == 2;
    }

    private boolean biomassBarRequiresUpdate()
    {
        return barCapacity.requiresUpdate() || barCriticalCapacity.requiresUpdate() || barCurrentBiomass.requiresUpdate() || barAbilityCost > 0D || barInsufficientFlash > 0;
    }

    public void updateBiomass(PlayerMorphData morphData)
    {
        barCapacity.updateTarget(morphData.getBiomassUpgradeValue(Upgrades.ID_BIOMASS_CAPACITY));
        barCriticalCapacity.updateTarget(morphData.getBiomassUpgradeValue(Upgrades.ID_BIOMASS_CRITICAL_CAPACITY));
        barCurrentBiomass.updateTarget(morphData.biomass);
    }

    public void clean()
    {
        morphStates.clear();
    }

    public void destroy()
    {
        //TODO required??
    }

    //helper func
    private PlayerMorphData getMorphData()
    {
        return Morph.eventHandlerClient.morphData;
    }

    @SubscribeEvent
    public void onRenderTick(RenderFrameEvent.Pre event)
    {
        {
            if((showSelector || showRadial) && mc.screen instanceof PauseScreen)
            {
                mc.setScreen(null);

                if(showSelector)
                {
                    closeSelector();
                }
                else
                {
                    closeRadial();
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event)
    {
        {
            tick();
        }
    }

    @SubscribeEvent
    public void onIngameGuiPre(RenderGuiEvent.Pre event)
    {
        {
            // event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onIngameGuiPost(RenderGuiEvent.Post event)
    {
        {
            if(shouldRenderSelector())
            {
                drawSelector(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaTicks(), mc.getWindow());
            }
            if(showRadial)
            {
                drawRadial(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaTicks(), mc.getWindow());
            }
        }
    }

    public static void renderMorphEntity(GuiGraphics guiGraphics, LivingEntity living, double x, double y, double z, float scale)
    {
        // Reset entity rotation and walk animation so it shows neutrally in the GUI.
        // Math.PI on Z is intentional - it rotates the entity to face the camera in inventory space.
        float savedYRot = living.getYRot();
        float savedXRot = living.getXRot();
        float savedYHead = living.yHeadRot;
        float savedYBody = living.yBodyRot;
        float savedYRotO = living.yRotO;

        living.setYRot(180F);
        living.setXRot(0F);
        living.yHeadRot = 180F;
        living.yBodyRot = 180F;
        living.yRotO = 180F;
        // Always show idle stance in selector - set walk animation to stopped
        living.walkAnimation.update(0F, 0F);

        float drawScale = Math.max(scale, 0.01F);
        InventoryScreen.renderEntityInInventory(guiGraphics, (float)x, (float)y, drawScale, new org.joml.Vector3f(), new org.joml.Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float)Math.PI), new org.joml.Quaternionf(), living);

        living.setYRot(savedYRot);
        living.setXRot(savedXRot);
        living.yHeadRot = savedYHead;
        living.yBodyRot = savedYBody;
        living.yRotO = savedYRotO;
        // Note: we do NOT restore walkAnimation - the GUI entities should always be idle
    }

    public void drawBiomassBar(GuiGraphics guiGraphics, int x, int y, float partialTick, float alpha)
    {
        // Stubbed for 1.21.1
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event)
    {
        if(event.getLevel() instanceof net.minecraft.world.level.Level && ((net.minecraft.world.level.Level)event.getLevel()).isClientSide())
        {
            clean();
        }
    }

    @SubscribeEvent
    public void onRawMouseInput(InputEvent.MouseButton.Pre event)
    {
        if(Morph.configClient.selectorAllowMouseControl && event.getAction() == GLFW.GLFW_PRESS)
        {
            if(showSelector)
            {
                // event.setCanceled(true);

                if(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT)
                {
                    confirmSelector();
                }
                else if(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                {
                    closeSelector();
                }
                else if(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
                {
                    toggleFavourite();
                }
            }
            else if(showRadial)
            {
                // event.setCanceled(true);

                if(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT)
                {
                    confirmRadial();
                }
                else if(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT || event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
                {
                    closeRadial();
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseScroll(MouseScrollingEvent event)
    {
        if(Morph.configClient.selectorAllowMouseControl && showSelector && event.getScrollDeltaY() != 0)
        {
            // event.setCanceled(true);

            shiftIndexSelector(event.getScrollDeltaY() < 0, Screen.hasShiftDown());
        }
    }

    public static void restoreShadowSize(PlayerRenderer renderer)
    {
        /*
        if(playerShadowSize == -1F)
        {
            // playerShadowSize = renderer.shadowRadius;
        }

        if(changedShadowSize)
        {
            changedShadowSize = false;
            // renderer.shadowRadius = playerShadowSize;
        }
        */
    }

    public static void setShadowSize(PlayerRenderer renderer, MorphInfo info, float partialTick)
    {
        /*
        float morphProgress = info.getMorphProgress(partialTick);
        if(morphProgress < 1F) //midmorph
        {
            float prevSize = info.prevState.renderedShadowSize;
            float nextSize = info.nextState.renderedShadowSize;

            // renderer.shadowRadius = prevSize + (nextSize - prevSize) * info.getTransitionProgressSine(partialTick);
        }
        else
        {
            // renderer.shadowRadius = info.nextState.renderedShadowSize;
        }

        changedShadowSize = true;
        */
    }

    public enum RadialMode
    {
        FAVOURITE,
        ABILITY
    }

    private static class BiomassValue
    {
        private double target;
        private double current;
        private double last;

        private BiomassValue(double value)
        {
            set(value);
        }

        private void set(double value)
        {
            target = current = last = value;
        }

        private void updateTarget(double value)
        {
            target = value;
        }

        private void tick()
        {
            last = current;

            if(current != target)
            {
                if(Math.abs(current - target) < 0.01F)
                {
                    current = target;
                }
                else
                {
                    current += (target - current) * 0.3F;
                }
            }
        }

        private boolean requiresUpdate()
        {
            return current != target;
        }

        private double getDisplayValue(float partialTick)
        {
            return last + (current - last) * partialTick;
        }
        //TODO requires update
    }
}
