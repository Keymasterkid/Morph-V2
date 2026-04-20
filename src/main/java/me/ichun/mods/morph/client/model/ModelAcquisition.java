package me.ichun.mods.morph.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.morph.client.entity.EntityAcquisition;
import me.ichun.mods.morph.client.render.RenderEntityAcquisition;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;

import java.util.ArrayList;

public class ModelAcquisition extends EntityModel<RenderEntityAcquisition.AcquisitionRenderState>
{
    public ModelAcquisition(ModelPart root)
    {
        super(root, RenderType::entityTranslucent);
    }



    public void render(EntityAcquisition entity, float partialTick, PoseStack stack, VertexConsumer buffer, int light, int overlay)
    {
        boolean isFirstPerson = entity.livingOrigin == net.minecraft.client.Minecraft.getInstance().cameraEntity && net.minecraft.client.Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;

        if(!entity.livingOrigin.isInvisible() || Morph.configServer.biomassSkinWhilstInvisible)
        {
            for(EntityAcquisition.Tendril tendril : entity.tendrils)
            {
                if(!tendril.isDone())
                {
                    tendril.renderTendril(entity, stack, buffer, light, overlay, partialTick);
                    tendril.renderCapture(entity, stack, buffer, light, overlay, partialTick);
                }
            }
        }
        else
        {
            for(EntityAcquisition.Tendril tendril : entity.tendrils)
            {
                if(!tendril.isDone())
                {
                    tendril.renderCapture(entity, stack, buffer, light, overlay, partialTick);
                }
            }
        }
    }

    @Override
    public void setupAnim(RenderEntityAcquisition.AcquisitionRenderState state)
    {
    }
}
