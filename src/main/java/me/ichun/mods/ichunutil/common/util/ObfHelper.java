package me.ichun.mods.ichunutil.common.util;
import net.neoforged.fml.loading.FMLLoader;
public class ObfHelper {
    private static boolean devEnvironment;
    public static void detectDevEnvironment() {
        devEnvironment = !net.neoforged.fml.loading.FMLLoader.isProduction();
    }
    public static boolean isDevEnvironment() { return devEnvironment; }

    public static final String getHurtSound = "getHurtSound";
    public static final String getDeathSound = "getDeathSound";
    public static final String getSoundVolume = "getSoundVolume";
    public static final String getSoundPitch = "getSoundPitch";
    public static final String onChangedPotionEffect = "onChangedPotionEffect";
}
