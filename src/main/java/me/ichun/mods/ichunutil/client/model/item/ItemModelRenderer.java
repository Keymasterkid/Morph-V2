package me.ichun.mods.ichunutil.client.model.item;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.ichunutil.client.item.ItemEffectHandler;
import me.ichun.mods.ichunutil.common.iChunUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import net.neoforged.neoforge.client.ClientHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;

import static net.minecraft.world.item.ItemDisplayContext.*;

@SuppressWarnings("deprecation")
abstract class ItemModelPart implements ResolvedModel
{
    private static final List<BakedQuad> EMPTY_LIST = Collections.emptyList();

    @Nonnull
    private final IModel model;

    public <T extends IModel> ItemModelPart(@Nonnull T renderer)
    {
        model = renderer;
    }

    /* @Override */
    public QuadCollection bakeTopGeometry(TextureSlots.Resolver resolver, ModelBaker baker, ModelState state, ModelDebugName name)
    {
        return QuadCollection.EMPTY;
    }

    /* @Override */
    public boolean getTopAmbientOcclusion()
    {
        return true;
    }

    /* @Override */
    public ItemTransforms getTopTransforms()
    {
        return model.getCameraTransforms();
    }

    // ItemModel implementation logic
    public void update(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, ClientLevel level, LivingEntity entity, int seed)
    {
        model.handleItemState(stack, level, entity);
        // Transform handling usually happens via the RenderState or baked into the model now, 
        // but for legacy support we trigger the perspective handle if needed.
        model.handlePerspective(context, new PoseStack()); 
    }


    // Legacy handler removed as functionality moved to ItemModel#update


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
