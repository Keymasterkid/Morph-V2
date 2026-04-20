package me.ichun.mods.ichunutil.client.model.tabula;
import net.minecraft.client.model.Model;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.common.module.tabula.project.Project;
public class ModelRotationPoint extends Model {
    public ModelRotationPoint(Project.Part part, int w, int h) { super(null, net.minecraft.client.renderer.RenderType::entityCutout); }
    public void render(PoseStack p, VertexConsumer v, int i1, int i2, int i3) {}
}
