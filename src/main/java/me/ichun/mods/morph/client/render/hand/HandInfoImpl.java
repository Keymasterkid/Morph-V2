package me.ichun.mods.morph.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.HumanoidArm;
import me.ichun.mods.ichunutil.client.model.TabulaModelRenderer;

public class HandInfoImpl implements HandHandler.HandInfo {
    public Class<? extends EntityModel> modelClass;
    public String[] parts;

    public HandInfoImpl() {}

    @Override
    public TabulaModelRenderer[] getHandParts(HumanoidArm arm, EntityModel model) {
        return new TabulaModelRenderer[0];
    }

    @Override
    public PoseStack[] getPlacementCorrectors(HumanoidArm arm) {
        return new PoseStack[0];
    }

    @Override
    public boolean setup() {
        return true;
    }

    @Override
    public Class<? extends EntityModel> getModelClass() {
        return modelClass;
    }
}
