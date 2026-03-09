package me.ichun.mods.ichunutil.client.model.item;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.ichunutil.client.item.ItemEffectHandler;
import me.ichun.mods.ichunutil.common.iChunUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import net.neoforged.neoforge.client.ClientHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static net.minecraft.world.item.ItemDisplayContext.*;

@SuppressWarnings("deprecation")
abstract class ItemModelPart implements BakedModel
{
    private static final List<BakedQuad> EMPTY_LIST = Collections.emptyList();

    @Nonnull
    private final IModel model;

    public <T extends BlockEntityWithoutLevelRenderer & IModel> ItemModelPart(@Nonnull T renderer)
    {
        model = renderer;
    }

    /* @Override */
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand)
    {
        return EMPTY_LIST;
    }

    /* @Override */
    public boolean isAmbientOcclusion()
    {
        return true;
    }

    /* @Override */
    public boolean isGui3d()
    {
        return true;
    }

    /* @Override */
    public boolean isSideLit()
    {
        return true;
    }

    /* @Override */
    public boolean isBuiltInRenderer()
    {
        return true;
    }

    /* @Override */
    public TextureAtlasSprite getParticleTexture()
    {
        return net.minecraft.client.Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).getSprite(MissingTextureAtlasSprite.getLocation()); //TODO do I have to generate a particle texture sprite for Block models?
    }

    /* @Override */
    public ItemTransforms getTransforms()
    {
        return model.getCameraTransforms();
    }

    /* @Override */
    public BakedModel applyTransform(ItemDisplayContext cameraItemDisplayContext, PoseStack stack, boolean applyLeftHandTransform)
    {
        model.handlePerspective(cameraItemDisplayContext, stack);

        //item can't be used animation
        if(model.isDualHanded())
        {
            boolean isLeft = isLeftHand(cameraItemDisplayContext);
            if(isFirstPerson(cameraItemDisplayContext) && ItemEffectHandler.dualHandedAnimationRight > 0)
            {
                float prog = (float)Math.sin(Mth.clamp((isLeft ? Mth.lerp(me.ichun.mods.ichunutil.client.core.ClientSetup.eventHandlerClient.partialTick, ItemEffectHandler.prevDualHandedAnimationLeft, ItemEffectHandler.dualHandedAnimationLeft) : Mth.lerp(me.ichun.mods.ichunutil.client.core.ClientSetup.eventHandlerClient.partialTick, ItemEffectHandler.prevDualHandedAnimationRight, ItemEffectHandler.dualHandedAnimationRight)) / (float)ItemEffectHandler.dualHandedAnimationTime, 0F, 1F) * Math.PI / 4F);
                stack.mulPose(com.mojang.math.Axis.XN.rotationDegrees(30F * prog));
                stack.translate(0F, -0.1F * prog, 0.3F * prog);
                stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(35F * prog));
            }
        }
        // ClientHooks.handlePerspective(this, cameraItemDisplayContext, stack); -> 1.21.1 BakedModel#applyTransform
        // actually just return this if we handle transform here
        return this;
    }

    /* @Override */
    public ItemOverrides getOverrides()
    {
        return ItemOverridesHandler.INSTANCE.setItemModel(this);
    }

    private static final class ItemOverridesHandler extends ItemOverrides
    {
        private static final ItemOverridesHandler INSTANCE = new ItemOverridesHandler();

        private ItemOverridesHandler()
        {
            super();
        }

        private ItemModelPart itemModel;

        private ItemOverridesHandler setItemModel(ItemModelPart itemModel)
        {
            this.itemModel = itemModel;
            return this;
        }

        /* @Override */
        public BakedModel getOverrideModel(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity) //getModelWithOverrides
        {
            itemModel.model.handleItemState(stack, world, entity);
            return originalModel;
        }
    }

    public static boolean isFirstPerson(ItemDisplayContext type)
    {
        return type == FIRST_PERSON_LEFT_HAND || type == FIRST_PERSON_RIGHT_HAND;
    }

    public static boolean isThirdPerson(ItemDisplayContext type)
    {
        return type == THIRD_PERSON_LEFT_HAND || type == THIRD_PERSON_RIGHT_HAND;
    }

    public static boolean isEntityRender(ItemDisplayContext type)
    {
        return isFirstPerson(type) || isThirdPerson(type);
    }

    public static boolean isLeftHand(ItemDisplayContext type)
    {
        return type == FIRST_PERSON_LEFT_HAND || type == THIRD_PERSON_LEFT_HAND;
    }

    public static boolean isRightHand(ItemDisplayContext type)
    {
        return type == FIRST_PERSON_RIGHT_HAND || type == THIRD_PERSON_RIGHT_HAND;
    }

    public static boolean isItemRender(ItemDisplayContext type) //default render type
    {
        return type == null || type == HEAD || type == GUI || type == GROUND || type == NONE;
    }
}
