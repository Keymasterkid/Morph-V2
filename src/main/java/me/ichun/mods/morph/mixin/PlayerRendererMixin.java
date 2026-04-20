package me.ichun.mods.morph.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin
{
    @Inject(method = "renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;Z)V", at = @At("HEAD"), cancellable = true)
    private void morph$renderRightHand(PoseStack stack, MultiBufferSource buffer, int light, ResourceLocation skinTexture, boolean slim, CallbackInfo ci)
    {
        if (Minecraft.getInstance().player instanceof LocalPlayer localPlayer) {
            PlayerRenderer renderer = (PlayerRenderer) (Object) this;
            if (me.ichun.mods.morph.client.render.hand.HandHandler.instance != null &&
                me.ichun.mods.morph.client.render.hand.HandHandler.instance.renderInteractionHand(
                    renderer, stack, buffer, light, localPlayer,
                    renderer.getModel().rightArm, renderer.getModel().rightSleeve)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;Z)V", at = @At("HEAD"), cancellable = true)
    private void morph$renderLeftHand(PoseStack stack, MultiBufferSource buffer, int light, ResourceLocation skinTexture, boolean slim, CallbackInfo ci)
    {
        if (Minecraft.getInstance().player instanceof LocalPlayer localPlayer) {
            PlayerRenderer renderer = (PlayerRenderer) (Object) this;
            if (me.ichun.mods.morph.client.render.hand.HandHandler.instance != null &&
                me.ichun.mods.morph.client.render.hand.HandHandler.instance.renderInteractionHand(
                    renderer, stack, buffer, light, localPlayer,
                    renderer.getModel().leftArm, renderer.getModel().leftSleeve)) {
                ci.cancel();
            }
        }
    }
}
