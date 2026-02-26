package me.ichun.mods.morph.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelRendererMixin
{
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"), cancellable = true)
    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, int color, CallbackInfo ci)
    {
        if(MorphRenderHandler.currentCapture != null)
        {
            if(!((ModelPart)(Object)this).visible)
            {
                ci.cancel();
                return;
            }
            matrixStackIn.pushPose();
            ((ModelPart)(Object)this).translateAndRotate(matrixStackIn);
            MorphRenderHandler.currentCapture.capture((ModelPart)(Object)this, matrixStackIn);

            for(net.minecraft.client.model.geom.ModelPart modelrenderer : ((ModelPartAccessor)(Object)this).getChildren().values()) {
                modelrenderer.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, color);
            }

            matrixStackIn.popPose();
            ci.cancel();
        }
    }
}
