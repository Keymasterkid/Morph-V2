package me.ichun.mods.morph.client.core;

import me.ichun.mods.ichunutil.client.key.KeyBind;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class KeyBinds
{
    public static KeyBind keySelectorUp;
    public static KeyBind keySelectorDown;
    public static KeyBind keySelectorLeft;
    public static KeyBind keySelectorRight;
    public static KeyBind keyFavourite;
    public static KeyBind keyAbility;
    public static KeyBind keyBiomass;

    public static void init()
    {
        if (keySelectorUp != null)
        {
            return;
        }
        keySelectorUp = new KeyBind(new KeyMapping("morph.key.selectorUp", KeyBind.ConflictContext.IN_GAME_MODIFIER_SENSITIVE, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET), "key.categories.morph"), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, false), null).setHoldable();
        keySelectorDown = new KeyBind(new KeyMapping("morph.key.selectorDown", KeyBind.ConflictContext.IN_GAME_MODIFIER_SENSITIVE, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET), "key.categories.morph"), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, false), null).setHoldable();
        keySelectorLeft = new KeyBind(new KeyMapping("morph.key.selectorLeft", KeyBind.ConflictContext.IN_GAME_MODIFIER_SENSITIVE, KeyModifier.SHIFT, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET), "key.categories.morph"), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, false), null).setHoldable();
        keySelectorRight = new KeyBind(new KeyMapping("morph.key.selectorRight", KeyBind.ConflictContext.IN_GAME_MODIFIER_SENSITIVE, KeyModifier.SHIFT, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET), "key.categories.morph"), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, false), null).setHoldable();
        keyFavourite = new KeyBind(new KeyMapping("morph.key.favourite", KeyConflictContext.IN_GAME, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_GRAVE_ACCENT), "key.categories.morph"), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, false), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, true));
        //Too many questions asking why it's "disabled"
        keyAbility = new KeyBind(new KeyMapping("morph.key.ability", KeyBind.ConflictContext.IN_GAME_MODIFIER_SENSITIVE, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B), "key.categories.morph"), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, false), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, true));
        keyBiomass = new KeyBind(new KeyMapping("morph.key.biomass", KeyBind.ConflictContext.IN_GAME_MODIFIER_SENSITIVE, KeyModifier.SHIFT, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B), "key.categories.morph"), keyBind -> Morph.eventHandlerClient.handleInput(keyBind, false), null);
    }

}
