package me.ichun.mods.ichunutil.client.gui.config.window;

import me.ichun.mods.ichunutil.client.gui.bns.window.Window;
import me.ichun.mods.ichunutil.client.gui.config.WorkspaceConfigs;
import me.ichun.mods.ichunutil.client.gui.config.window.view.ViewValues;

public class WindowValues extends Window<WorkspaceConfigs>
{


    private net.minecraft.client.gui.components.events.GuiEventListener focused;
    private boolean isDragging;
    public void setFocused(net.minecraft.client.gui.components.events.GuiEventListener l) {
        net.minecraft.client.gui.components.events.GuiEventListener lastFocused = this.focused;
        if (lastFocused instanceof me.ichun.mods.ichunutil.client.gui.bns.window.Fragment && lastFocused != l) {
            ((me.ichun.mods.ichunutil.client.gui.bns.window.Fragment<?>)lastFocused).unfocus(l);
        }
        this.focused = l;
    }
    public net.minecraft.client.gui.components.events.GuiEventListener getFocused() { return focused; }
    public boolean isDragging() { return isDragging; }
    public void setDragging(boolean isDragging) { this.isDragging = isDragging; }

    public WindowValues(WorkspaceConfigs parent, WorkspaceConfigs.ConfigInfo info, String category)
    {
        super(parent);
        setView(new ViewValues(this, info.config.getConfigName() + " - " + WorkspaceConfigs.getLocalizedCategory(info, category, "name"), info, category, info.categories.get(category)));
        pos(20, 20);
        size(200, 300);
        disableUndocking();
        disableDrag();
        disableBringToFront();
    }
}
