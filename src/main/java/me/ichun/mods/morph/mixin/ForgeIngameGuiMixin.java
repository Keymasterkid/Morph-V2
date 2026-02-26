package me.ichun.mods.morph.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.Gui.class) //I'm sorry NeoForge!!
public abstract class ForgeIngameGuiMixin
{
    @Inject(method = "render", at = @At("HEAD"))
    public void renderPre(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci)
    {
        if(Morph.eventHandlerClient != null && Morph.eventHandlerClient.hudHandler != null)
        {
            // Morph.eventHandlerClient.hudHandler.preDrawBiomassBar(guiGraphics, deltaTracker);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void renderPost(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci)
    {
        if(Morph.eventHandlerClient != null && Morph.eventHandlerClient.hudHandler != null)
        {
            // Morph.eventHandlerClient.hudHandler.postDrawBiomassBar(guiGraphics, deltaTracker);
        }
    }
}
