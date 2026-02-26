package me.ichun.mods.morph.client.gui.biomass;

import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.Workspace;
import me.ichun.mods.ichunutil.client.gui.bns.window.constraint.Constraint;
import me.ichun.mods.morph.client.gui.biomass.scene.Scene;
import me.ichun.mods.morph.client.gui.biomass.scene.SceneBiomassAbilities;
import me.ichun.mods.morph.client.gui.biomass.scene.SceneBiomassUpgrades;
import me.ichun.mods.morph.client.gui.biomass.scene.SceneMorphs;
import me.ichun.mods.morph.client.gui.biomass.window.WindowHeader;
import me.ichun.mods.morph.client.gui.biomass.window.WindowSidebar;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

import java.text.DecimalFormat;

public class WorkspaceMorph extends Workspace
{
    public static final DecimalFormat FORMATTER = new DecimalFormat("#,###.##");
    public static final int PADDING_VERTICAL = 15;
    public static final int PADDING_WINDOW = 2;

    public Scene sceneBiomassUpgrades;
    public Scene sceneBiomassAbilities;
    public Scene sceneMorphs;

    private Scene currentScene;

    public WindowHeader windowHeader;
    public WindowSidebar windowSidebar;

    public WorkspaceMorph(Screen lastScreen)
    {
        super(lastScreen, Component.translatable("morph.gui.workspace.title"), Morph.configClient.guiMinecraftStyle);

        windowHeader = new WindowHeader(this);
        windowHeader.size(0, 20);
        windowHeader.constraints().top(this, Constraint.Property.Type.TOP, PADDING_VERTICAL).width(this, Constraint.Property.Type.WIDTH, 60);
        windows.add(windowHeader); //add to end of list

        windowSidebar = new WindowSidebar(this);
        windowSidebar.size(22, 0);
        windowSidebar.constraints().left(windowHeader, Constraint.Property.Type.LEFT, 0).top(windowHeader, Constraint.Property.Type.BOTTOM, PADDING_WINDOW).bottom(this, Constraint.Property.Type.BOTTOM, PADDING_VERTICAL);
        windows.add(windowSidebar); //add to end of list

        sceneBiomassUpgrades = new SceneBiomassUpgrades(this);
        sceneBiomassAbilities = new SceneBiomassAbilities(this);
        sceneMorphs = new SceneMorphs(this);

        currentScene = sceneBiomassUpgrades;
        currentScene.addWindows(this);
        //no need to call init, we haven't even inited yet
    }

    public void setScene(Scene scene) //TODO why not just swap views??
    {
        if(currentScene != scene)
        {
            currentScene.removeWindows(this);

            scene.addWindows(this); //call before assigning so the scene has reference to the previous scene.
            scene.init();
            currentScene = scene;
        }
    }

    @Override
    public boolean canDockWindows()
    {
        return false;
    }

    @Override
    public void renderWindows(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        for(int i = windows.size() - 1; i >= 0; i--)
        {
            windows.get(i).render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if(renderMinecraftStyle > 0)
        {
            guiGraphics.fill(0, 0, width, height, 0x80000000);
        }
    }
}
