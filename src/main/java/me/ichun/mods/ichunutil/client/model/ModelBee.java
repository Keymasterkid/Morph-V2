package me.ichun.mods.ichunutil.client.model;
import net.minecraft.client.model.Model;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
public class ModelBee extends Model {
    public static final ResourceLocation TEX_BEE = ResourceLocation.fromNamespaceAndPath("ichunutil", "textures/model/bee.png");
    public ModelBee(net.minecraft.client.model.geom.ModelPart root) { super(root, net.minecraft.client.renderer.RenderType::entityCutoutNoCull); }
}
