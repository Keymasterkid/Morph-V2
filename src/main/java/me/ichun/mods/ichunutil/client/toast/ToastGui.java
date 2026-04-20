package me.ichun.mods.ichunutil.client.toast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
public class ToastGui extends ToastManager {
    public ToastGui(Minecraft mc) { super(mc, mc.options); }
}
