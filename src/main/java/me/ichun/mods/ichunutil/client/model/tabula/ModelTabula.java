package me.ichun.mods.ichunutil.client.model.tabula;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.client.model.TabulaModelRenderer;
import me.ichun.mods.ichunutil.client.model.util.ModelHelper;
import me.ichun.mods.ichunutil.common.module.tabula.project.Project;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.List;

public class ModelTabula extends Model
{
    public final Project project;
    public final List<TabulaModelRenderer> models = new ArrayList<>();
    public boolean isDirty = true;

    public ModelTabula(Project project)
    {
        super(null, net.minecraft.client.renderer.RenderType::entityCutout);
        this.project = project;
    }

    public void createParts()
    {
        models.clear();
        project.parts.forEach(part -> models.add(ModelHelper.createModelPart(part, true)));
    }

    public void renderTabula(PoseStack matrixStack, VertexConsumer buffer, int light, int overlay, int color)
    {
        if(isDirty)
        {
            isDirty = false;
            createParts();
        }

        // Color in 1.21.1 is packed ARGB. We need to extract floats.
        float a = ((color >> 24) & 0xFF) / 255F;
        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        models.forEach(model -> model.render(matrixStack, buffer, light, overlay, r, g, b, a));
    }
}
