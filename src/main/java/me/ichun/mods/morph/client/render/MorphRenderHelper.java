package me.ichun.mods.morph.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import java.lang.reflect.Field;

public class MorphRenderHelper
{
    private static Field shadowRadiusField;

    static {
        try {
            shadowRadiusField = EntityRenderer.class.getDeclaredField("shadowRadius");
            shadowRadiusField.setAccessible(true);
        } catch (Exception e) {
            try {
                // MCP/SRG name fallback if needed, but in 1.21.1 we usually have mojmaps.
                // However, let's try the common obfuscated name just in case if this was production.
                // For now, assume mojmaps.
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    public static float getShadowRadius(EntityRenderer<?> renderer) {
        try {
            return shadowRadiusField.getFloat(renderer);
        } catch (Exception e) {
            return 0.75F; // Fallback
        }
    }

    public static void setShadowRadius(EntityRenderer<?> renderer, float radius) {
        try {
            shadowRadiusField.setFloat(renderer, radius);
        } catch (Exception e) {
            // Silently fail
        }
    }
}
