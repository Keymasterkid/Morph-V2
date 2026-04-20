package me.ichun.mods.ichunutil.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class TabulaModelRenderer {
    public float textureWidth = 64.0F;
    public float textureHeight = 32.0F;
    public int textureOffsetX;
    public int textureOffsetY;

    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;

    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;

    public boolean mirror;
    public boolean showModel = true;

    public final List<ModelBox> cubeList = new ArrayList<>();
    public final List<TabulaModelRenderer> childModels = new ArrayList<>();

    public TabulaModelRenderer(int textureWidthIn, int textureHeightIn, int textureOffsetXIn, int textureOffsetYIn) {
        this.textureWidth = textureWidthIn;
        this.textureHeight = textureHeightIn;
        this.textureOffsetX = textureOffsetXIn;
        this.textureOffsetY = textureOffsetYIn;
    }

    public TabulaModelRenderer(int texOffX, int texOffY) {
        this(64, 32, texOffX, texOffY);
    }

    public void addChild(TabulaModelRenderer child) {
        this.childModels.add(child);
    }

    public void setTextureOffset(int x, int y) {
        this.textureOffsetX = x;
        this.textureOffsetY = y;
    }

    public void addBox(float x, float y, float z, float width, float height, float depth) {
        this.addBox(x, y, z, width, height, depth, 0.0F);
    }

    public void addBox(float x, float y, float z, float width, float height, float depth, float delta) {
        this.addBox(x, y, z, width, height, depth, delta, delta, delta);
    }

    public void addBox(float x, float y, float z, float width, float height, float depth, float deltaX, float deltaY, float deltaZ) {
        this.cubeList.add(new ModelBox(this, this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, deltaX, deltaY, deltaZ, this.mirror, this.textureWidth, this.textureHeight));
    }

    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, int color) {
        if (this.showModel) {
            if (!this.cubeList.isEmpty() || !this.childModels.isEmpty()) {
                matrixStackIn.pushPose();
                this.translateRotate(matrixStackIn);
                this.doRender(matrixStackIn.last(), bufferIn, packedLightIn, packedOverlayIn, color);

                for (TabulaModelRenderer child : this.childModels) {
                    child.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, color);
                }

                matrixStackIn.popPose();
            }
        }
    }

    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, net.minecraft.util.ARGB.color((int)(alpha * 255), (int)(red * 255), (int)(green * 255), (int)(blue * 255)));
    }

    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn) {
        this.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void translateRotate(PoseStack matrixStackIn) {
        matrixStackIn.translate((double) (this.rotationPointX / 16.0F), (double) (this.rotationPointY / 16.0F), (double) (this.rotationPointZ / 16.0F));
        if (this.rotateAngleZ != 0.0F) {
            matrixStackIn.mulPose(com.mojang.math.Axis.ZP.rotation(this.rotateAngleZ));
        }

        if (this.rotateAngleY != 0.0F) {
            matrixStackIn.mulPose(com.mojang.math.Axis.YP.rotation(this.rotateAngleY));
        }

        if (this.rotateAngleX != 0.0F) {
            matrixStackIn.mulPose(com.mojang.math.Axis.XP.rotation(this.rotateAngleX));
        }
    }

    public void doRender(PoseStack.Pose matrixEntryIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, int color) {
        Matrix4f matrix4f = matrixEntryIn.pose();
        Matrix3f matrix3f = matrixEntryIn.normal();

        for (ModelBox modelbox : this.cubeList) {
            for (TexturedQuad texturedquad : modelbox.quads) {
                Vector3f vector3f = new Vector3f(texturedquad.normal);
                vector3f.mul(matrix3f);
                float f = vector3f.x();
                float f1 = vector3f.y();
                float f2 = vector3f.z();

                for (int i = 0; i < 4; ++i) {
                    PositionTextureVertex vertex = texturedquad.vertexPositions[i];
                    float f3 = vertex.position.x() / 16.0F;
                    float f4 = vertex.position.y() / 16.0F;
                    float f5 = vertex.position.z() / 16.0F;
                    Vector4f vector4f = new Vector4f(f3, f4, f5, 1.0F);
                    vector4f.mul(matrix4f);
                    bufferIn.addVertex(vector4f.x(), vector4f.y(), vector4f.z()).setColor(color).setUv(vertex.textureU, vertex.textureV).setOverlay(packedOverlayIn).setLight(packedLightIn).setNormal(matrixEntryIn, f, f1, f2);
                }
            }
        }
    }

    public void doRender(PoseStack.Pose matrixEntryIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.doRender(matrixEntryIn, bufferIn, packedLightIn, packedOverlayIn, net.minecraft.util.ARGB.color((int)(alpha * 255), (int)(red * 255), (int)(green * 255), (int)(blue * 255)));
    }

    public static class ModelBox {
        public final float posX1;
        public final float posY1;
        public final float posZ1;
        public final float posX2;
        public final float posY2;
        public final float posZ2;
        public final TexturedQuad[] quads;

        public ModelBox(TabulaModelRenderer renderer, int texOffX, int texOffY, float x, float y, float z, float width, float height, float depth, float deltaX, float deltaY, float deltaZ, boolean mirror, float texWidth, float texHeight) {
            this.posX1 = x;
            this.posY1 = y;
            this.posZ1 = z;
            this.posX2 = x + width;
            this.posY2 = y + height;
            this.posZ2 = z + depth;
            this.quads = new TexturedQuad[6];
            float f = x + width;
            float f1 = y + height;
            float f2 = z + depth;
            x -= deltaX;
            y -= deltaY;
            z -= deltaZ;
            f += deltaX;
            f1 += deltaY;
            f2 += deltaZ;
            if (mirror) {
                float f3 = f;
                f = x;
                x = f3;
            }

            PositionTextureVertex ptv0 = new PositionTextureVertex(x, y, z, 0.0F, 0.0F);
            PositionTextureVertex ptv1 = new PositionTextureVertex(f, y, z, 0.0F, 8.0F);
            PositionTextureVertex ptv2 = new PositionTextureVertex(f, f1, z, 8.0F, 8.0F);
            PositionTextureVertex ptv3 = new PositionTextureVertex(x, f1, z, 8.0F, 0.0F);
            PositionTextureVertex ptv4 = new PositionTextureVertex(x, y, f2, 0.0F, 0.0F);
            PositionTextureVertex ptv5 = new PositionTextureVertex(f, y, f2, 0.0F, 8.0F);
            PositionTextureVertex ptv6 = new PositionTextureVertex(f, f1, f2, 8.0F, 8.0F);
            PositionTextureVertex ptv7 = new PositionTextureVertex(x, f1, f2, 8.0F, 0.0F);
            float f4 = (float) texOffX;
            float f5 = (float) texOffX + depth;
            float f6 = (float) texOffX + depth + width;
            float f7 = (float) texOffX + depth + width + width;
            float f8 = (float) texOffX + depth + width + depth;
            float f9 = (float) texOffX + depth + width + depth + width;
            float f10 = (float) texOffY;
            float f11 = (float) texOffY + depth;
            float f12 = (float) texOffY + depth + height;
            this.quads[2] = new TexturedQuad(new PositionTextureVertex[]{ptv5, ptv4, ptv0, ptv1}, f5, f10, f6, f11, texWidth, texHeight, mirror, 0.0F, -1.0F, 0.0F);
            this.quads[3] = new TexturedQuad(new PositionTextureVertex[]{ptv2, ptv3, ptv7, ptv6}, f6, f11, f7, f10, texWidth, texHeight, mirror, 0.0F, 1.0F, 0.0F);
            this.quads[1] = new TexturedQuad(new PositionTextureVertex[]{ptv0, ptv4, ptv7, ptv3}, f4, f11, f5, f12, texWidth, texHeight, mirror, -1.0F, 0.0F, 0.0F);
            this.quads[4] = new TexturedQuad(new PositionTextureVertex[]{ptv1, ptv0, ptv3, ptv2}, f5, f11, f6, f12, texWidth, texHeight, mirror, 0.0F, 0.0F, -1.0F);
            this.quads[0] = new TexturedQuad(new PositionTextureVertex[]{ptv5, ptv1, ptv2, ptv6}, f6, f11, f8, f12, texWidth, texHeight, mirror, 1.0F, 0.0F, 0.0F);
            this.quads[5] = new TexturedQuad(new PositionTextureVertex[]{ptv4, ptv5, ptv6, ptv7}, f8, f11, f9, f12, texWidth, texHeight, mirror, 0.0F, 0.0F, 1.0F);
        }
    }

    public static class TexturedQuad {
        public final PositionTextureVertex[] vertexPositions;
        public final Vector3f normal;

        public TexturedQuad(PositionTextureVertex[] positions, float u1, float v1, float u2, float v2, float texWidth, float texHeight, boolean mirror, float nx, float ny, float nz) {
            this.vertexPositions = positions;
            float f = 0.0F / texWidth;
            float f1 = 0.0F / texHeight;
            positions[0] = positions[0].setTextureUV(u2 / texWidth - f, v1 / texHeight + f1);
            positions[1] = positions[1].setTextureUV(u1 / texWidth + f, v1 / texHeight + f1);
            positions[2] = positions[2].setTextureUV(u1 / texWidth + f, v2 / texHeight - f1);
            positions[3] = positions[3].setTextureUV(u2 / texWidth - f, v2 / texHeight - f1);
            if (mirror) {
                int i = positions.length;
                for (int j = 0; j < i / 2; ++j) {
                    PositionTextureVertex ptv = positions[j];
                    positions[j] = positions[i - 1 - j];
                    positions[i - 1 - j] = ptv;
                }
            }

            this.normal = new Vector3f(nx, ny, nz);
            if (mirror) {
                this.normal.mul(-1.0F, 1.0F, 1.0F);
            }
        }
    }

    public static class PositionTextureVertex {
        public final Vector3f position;
        public final float textureU;
        public final float textureV;

        public PositionTextureVertex(float x, float y, float z, float u, float v) {
            this(new Vector3f(x, y, z), u, v);
        }

        public PositionTextureVertex(Vector3f pos, float u, float v) {
            this.position = pos;
            this.textureU = u;
            this.textureV = v;
        }

        public PositionTextureVertex setTextureUV(float u, float v) {
            return new PositionTextureVertex(this.position, u, v);
        }
    }
}
