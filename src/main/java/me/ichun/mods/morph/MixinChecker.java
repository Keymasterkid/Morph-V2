package me.ichun.mods.morph;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.model.geom.ModelPart;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class MixinChecker {
    public static void check() {
        List<String> lines = new ArrayList<>();
        checkClass(PlayerRenderer.class, lines);
        checkClass(Entity.class, lines);
        checkClass(LivingEntity.class, lines);
        checkClass(ModelPart.class, lines);
        
        try {
            Files.write(Paths.get("mixin_check.txt"), lines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkClass(Class<?> clazz, List<String> lines) {
        lines.add("--- " + clazz.getName() + " ---");
        for (Method m : clazz.getDeclaredMethods()) {
            lines.add(m.getName() + " " + Arrays.toString(m.getParameterTypes()));
        }
        if (clazz.getSuperclass() != null) {
            // checkClass(clazz.getSuperclass(), lines);
        }
    }
}
