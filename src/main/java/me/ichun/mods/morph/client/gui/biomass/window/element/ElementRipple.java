package me.ichun.mods.morph.client.gui.biomass.window.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.element.Element;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class ElementRipple extends Element<ElementBiomassUpgrades>
{
    // Updated RenderType for 1.21.1
    public final RenderType RIPPLE = RenderType.entityTranslucent(me.ichun.mods.morph.common.morph.MorphHandler.INSTANCE.getMorphSkinTexture());

    public int age;

    public ElementRipple(@Nonnull ElementBiomassUpgrades parent)
    {
        super(parent);
    }

    @Override
    public void tick()
    {
        age++;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        float prog = 10F * Mth.clamp((age + partialTick) / 20, 0F, 1F);
        double dist = ElementUpgradeNode.SIZE * 3.5D;

        if(prog >= 1F)
        {
            float alpha = 1F - EntityHelper.sineifyProgress(Mth.clamp((age - 10 + partialTick) / 10F, 0F, 1F));

            double travDist = dist * Math.log10(prog);
            int slices = 30; // Stubbed for simplicity

            org.joml.Matrix3x2fStack _m2d = guiGraphics.pose();
org.joml.Matrix4f matrix = new org.joml.Matrix4f(_m2d.m00(), _m2d.m01(), 0, 0, _m2d.m10(), _m2d.m11(), 0, 0, 0, 0, 1, 0, _m2d.m20(), _m2d.m21(), 0, 1);
            net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer builder = bufferSource.getBuffer(RIPPLE);
            
            for(int i = 0; i <= slices; i++)
            {
                double angle = Math.PI * 2 * i / slices;
                float x = (float)(Math.cos(angle) * travDist);
                float y = (float)(Math.sin(angle) * travDist);
                float x2 = (float)(Math.cos(angle) * (travDist + 2D));
                float y2 = (float)(Math.sin(angle) * (travDist + 2D));

                builder.addVertex(matrix, (float)posX + x, (float)posY + y, 0F).setColor(255, 255, 255, (int)(alpha * 255)).setUv(0, 0);
                builder.addVertex(matrix, (float)posX + x2, (float)posY + y2, 0F).setColor(255, 255, 255, (int)(alpha * 255)).setUv(0, 0);
            }
            
            // bufferSource.endBatch(RIPPLE);

            float nextProg = 10F * Mth.clamp((age + 1 + partialTick) / 20, 0F, 1F);
            double nextTravDist = dist * Math.log10(nextProg);

            Vec3 ourVec = getAsVector();
            ArrayList<ElementUpgradeNode> activeNodes = parentFragment.getActiveNodes();
            for(ElementUpgradeNode node : activeNodes)
            {
                double nodeDist = ourVec.distanceTo(node.getAsVector());
                if(nodeDist < dist && nodeDist < nextTravDist && nodeDist > travDist && nodeDist > ElementUpgradeNode.SIZE)
                {
                    Vec3 diff = getAsVector().subtract(node.getAsVector());
                    Vec3 normal = diff.normalize();
                    double mag = (dist - nodeDist) / dist * 0.5D;
                    Vec3 mul = normal.multiply(mag, mag, mag);
                    node.pushX -= mul.x;
                    node.pushY -= mul.y;
                }
            }
        }
    }

    public Vec3 getAsVector()
    {
        return new Vec3(posX, posY, 0D);
    }

    @Override
    public int getLeft()
    {
        return super.getLeft() + parentFragment.offsetX;
    }

    @Override
    public int getRight()
    {
        return super.getRight() + parentFragment.offsetX;
    }

    @Override
    public int getTop()
    {
        return super.getTop() + parentFragment.offsetY;
    }

    @Override
    public int getBottom()
    {
        return super.getBottom() + parentFragment.offsetY;
    }
}
