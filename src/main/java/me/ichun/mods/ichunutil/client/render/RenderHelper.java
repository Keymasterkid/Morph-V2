package me.ichun.mods.ichunutil.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;

public class RenderHelper {
    public static Collection<RenderTarget> frameBuffers = new ArrayList<>();

    public static void startGlScissor(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int) (x * scale),
                (int) (mc.getWindow().getHeight() - (y + h) * scale),
                (int) (w * scale),
                (int) (h * scale)
        );
    }

    public static void endGlScissor() {
        RenderSystem.disableScissor();
    }

    // --- Draw methods used by GUI element classes ---

    public static void draw(GuiGraphics guiGraphics, double x, double y, double w, double h, double z) {
        draw(guiGraphics, x, y, w, h, z, 0D, 0D, 1D, 1D);
    }

    // Track the last bound texture for use in draw() calls
    private static ResourceLocation lastBoundTexture = null;

    public static void setLastBoundTexture(ResourceLocation rl) {
        lastBoundTexture = rl;
    }

    public static void draw(GuiGraphics guiGraphics, double x, double y, double w, double h, double z, double u, double v, double uw, double vh) {
        if (lastBoundTexture != null) {
            // u/v are in normalized 0-1 range from the old code. Convert to pixel coords for blit.
            // The old code passes u/v as fractions of texture size.
            guiGraphics.blit(lastBoundTexture, (int) x, (int) y, (float) (u * 256), (float) (v * 256), (int) w, (int) h, 256, 256);
        }
    }





    // --- Batch drawing stubs (used by Element.cropAndStitch) ---

    public static void startDrawBatch() {
        // In 1.21.1, batch drawing is managed by GuiGraphics internally
    }

    public static void drawBatch(GuiGraphics guiGraphics, double x, double y, double w, double h, double z, double u, double v, double uw, double vh) {
        // Delegate to individual draw calls - GuiGraphics handles batching
        draw(guiGraphics, x, y, w, h, z, u, v, uw, vh);
    }

    public static void endDrawBatch() {
        // No-op in 1.21.1
    }

    // --- Colour drawing ---

    public static void drawColour(GuiGraphics guiGraphics, int colour, int alpha, double x, double y, double w, double h, double z) {
        guiGraphics.fill((int) x, (int) y, (int) (x + w), (int) (y + h), (alpha << 24) | (colour & 0xFFFFFF));
    }

    public static void drawColour(GuiGraphics guiGraphics, int r, int g, int b, int a, double x, double y, double w, double h, double z) {
        guiGraphics.fill((int) x, (int) y, (int) (x + w), (int) (y + h), (a << 24) | (r << 16) | (g << 8) | b);
    }

    // --- Texture drawing ---

    public static void drawTexture(GuiGraphics guiGraphics, ResourceLocation loc, double x, double y, double w, double h, double z) {
        guiGraphics.blit(loc, (int) x, (int) y, 0, 0, (int) w, (int) h, (int) w, (int) h);
    }

    public static void drawTexture(GuiGraphics guiGraphics, ResourceLocation loc, double x, double y, double w, double h, double z, double u, double v, double uw, double vh) {
        guiGraphics.blit(loc, (int) x, (int) y, (float) u, (float) v, (int) w, (int) h, (int) uw, (int) vh);
    }

    // --- Framebuffer management ---

    public static RenderTarget createFrameBuffer() {
        Minecraft mc = Minecraft.getInstance();
        TextureTarget render = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
        frameBuffers.add(render);
        return render;
    }

    public static void deleteFrameBuffer(RenderTarget buffer) {
        buffer.destroyBuffers();
        frameBuffers.remove(buffer);
    }

    // --- Pose interpolation ---

    public static PoseStack.Pose createInterimStackEntry(PoseStack.Pose prev, PoseStack.Pose next, float prog) {
        Matrix4f prevP = prev.pose();
        Matrix4f nextP = next.pose();
        Matrix3f prevN = prev.normal();
        Matrix3f nextN = next.normal();

        Matrix4f destP = new Matrix4f(prevP).lerp(nextP, prog);
        Matrix3f destN = new Matrix3f(prevN).lerp(nextN, prog);

        try {
            java.lang.reflect.Constructor<PoseStack.Pose> constructor = PoseStack.Pose.class.getDeclaredConstructor(Matrix4f.class, Matrix3f.class);
            constructor.setAccessible(true);
            return constructor.newInstance(destP, destN);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PoseStack.Pose", e);
        }
    }
}
