package me.ichun.mods.morph;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MethodScanner {
    public static void scan() {
        List<String> lines = new ArrayList<>();
        lines.add("--- Entity Methods ---");
        for (Method m : Entity.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("sound") || m.getName().toLowerCase().contains("step") || m.getName().toLowerCase().contains("swim") || m.getName().toLowerCase().contains("fly")) {
                lines.add(m.getName() + " " + Arrays.toString(m.getParameterTypes()));
            }
        }
        lines.add("\n--- LivingEntity Methods ---");
        for (Method m : LivingEntity.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("sound") || m.getName().toLowerCase().contains("step") || m.getName().toLowerCase().contains("swim") || m.getName().toLowerCase().contains("fly") || m.getName().toLowerCase().contains("drink") || m.getName().toLowerCase().contains("eat")) {
                lines.add(m.getName() + " " + Arrays.toString(m.getParameterTypes()));
            }
        }
        try {
            Files.write(Paths.get("class_scan.txt"), lines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
