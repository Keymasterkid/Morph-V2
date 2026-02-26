package me.ichun.mods.ichunutil.client.core;
public class ConfigClient {
    public int guiTooltipCooldown = 0;
    public int guiMinecraftStyle = 0; // Changed from boolean to int to match usage if needed, or fix usage
    public int guiDoubleClickSpeed = 500; // Changed from double to int
    public int guiDockBorder = 0;
    public int guiDockPadding = 0;
    public boolean overrideToastGui = false;
    public boolean morphDisableRidingPlayerRenderInFirstPerson = false;
    public ConfigClient init() { return this; }
}
