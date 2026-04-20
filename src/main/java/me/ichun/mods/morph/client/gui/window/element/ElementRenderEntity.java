package me.ichun.mods.morph.client.gui.window.element;

import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.element.Element;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.Util;
import org.lwjgl.opengl.GL11;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

public class ElementRenderEntity extends Element<Fragment>
{
    private static final Quaternionf ENTITY_ROTATION = new Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float)Math.PI);

    @Nonnull
    public LivingEntity entToRender;

    public float renderScale;

    public ElementRenderEntity(@Nonnull Fragment parent)
    {
        super(parent);
        renderScale = 1.0F;
    }

    public ElementRenderEntity(@Nonnull Fragment parent, float scale)
    {
        super(parent);
        renderScale = scale;
    }

    public ElementRenderEntity setEntityToRender(LivingEntity ent)
    {
        entToRender = ent;
        return this;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if(renderMinecraftStyle() > 0)
        {
            bindTexture(resourceHorse());
            cropAndStitch(guiGraphics, getLeft() - 1, getTop() - 1, width + 2, height + 2, 2, 79, 17, 90, 54, 256, 256);
        }
        else
        {
             guiGraphics.renderOutline(getLeft() - 1, getTop() - 1, width + 2, height + 2, 0xFF000000 | (getTheme().elementTreeBorder[0] << 16) | (getTheme().elementTreeBorder[1] << 8) | getTheme().elementTreeBorder[2]);
        }

        // RenderSystem.enableBlend();
        // RenderSystem.defaultBlendFunc();

        fill(guiGraphics, new int[]{0, 0, 0}, 255, 0);

        EntityDimensions livingSize = entToRender.getDimensions(net.minecraft.world.entity.Pose.STANDING);
        float entSize = Math.max(livingSize.width(), livingSize.height()) / 1.95F; //1.95F = zombie height

        float entScale = renderScale * (1F / Math.max(1F, entSize));

        renderEntity(guiGraphics, getLeft() + (width / 2D), getBottom() - 15 * renderScale, 30F * entScale);
    }

    private void renderEntity(GuiGraphics guiGraphics, double x, double y, float scale)
    {
        int ix = (int)x, iy = (int)y, iscale = (int)scale;
        InventoryScreen.renderEntityInInventory(guiGraphics, ix - iscale, iy - iscale, ix + iscale, iy + iscale, (float)iscale, new Vector3f(), ENTITY_ROTATION, new Quaternionf(), entToRender);
    }

    @Override
    public int getMinHeight()
    {
        return height;
    }
}
