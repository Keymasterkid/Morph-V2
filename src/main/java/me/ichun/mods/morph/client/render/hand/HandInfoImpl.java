package me.ichun.mods.morph.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.ichunutil.client.model.TabulaModelRenderer;
import me.ichun.mods.ichunutil.client.model.util.ModelHelper;
import me.ichun.mods.ichunutil.common.module.tabula.project.Project;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HandInfoImpl implements HandHandler.HandInfo {
    public String forClass;
    public PartData[] leftHandParts;
    public PartData[] rightHandParts;

    public static class CorrectorData {
        public String type;
        public String axis;
        public float amount;
    }

    public static class PartData {
        public String fieldName;
        public List<CorrectorData> placementCorrectors;
    }

    private Class<? extends EntityModel<?>> modelClass;

    public HandInfoImpl() {}

    @SuppressWarnings("unchecked")
    @Override
    public boolean setup() {
        if (forClass == null) return false;
        
        String className = forClass;
        if (className.startsWith("net.minecraft.client.renderer.entity.model.")) {
            // No changes usually needed for client models in 1.21 as they stayed there typically,
            // except that some names may have changed. 'BipedModel' -> 'HumanoidModel' etc.
            if (className.endsWith("BipedModel")) {
                className = "net.minecraft.client.model.HumanoidModel";
            } else {
                className = className.replace("net.minecraft.client.renderer.entity.model.", "net.minecraft.client.model.");
            }
        }

        try {
            Class<?> mapped = Class.forName(className);
            if (EntityModel.class.isAssignableFrom(mapped)) {
                modelClass = (Class<? extends EntityModel<?>>) mapped;
                return true;
            }
        } catch (ClassNotFoundException e) {
            Morph.LOGGER.debug("HandInfo model class not on classpath: {} -> {}", forClass, className);
        }
        return false;
    }

    @Override
    public TabulaModelRenderer[] getHandParts(HumanoidArm arm, EntityModel<?> model) {
        PartData[] targetParts = arm == HumanoidArm.LEFT ? leftHandParts : rightHandParts;
        List<ModelPart> matchedParts = new ArrayList<>();

        if (targetParts != null && targetParts.length > 0 && false) { 
            // In 1.21, reflection with 1.16 SRG field names (field_12345) will always fail.
            // We ignore JSON fieldNames and rely on the dynamic logical sweep below to guarantee compatibility.
        }
        
        // DYNAMIC LIMB SCANNER - 1.21.8 NeoForge
        ModelPart limb = scanForLimb(model, arm);
        if (limb != null) {
            matchedParts.add(limb);
        }

        TabulaModelRenderer[] result = new TabulaModelRenderer[matchedParts.size()];
        for (int i = 0; i < matchedParts.size(); i++) {
            Project.Part pPart = ModelHelper.createPartFor(matchedParts.get(i), true);
            result[i] = ModelHelper.createModelPart(pPart, true);
        }
        return result;
    }

    private ModelPart scanForLimb(EntityModel<?> model, HumanoidArm arm) {
        String handedness = arm == HumanoidArm.LEFT ? "left" : "right";
        String handKey = handedness + "_arm";
        String legKey = handedness + "_front_leg"; // for quadrupeds
        String wingKey = handedness + "_wing";

        ModelPart rootNode = null;
        try {
            Method rootMethod = model.getClass().getMethod("root");
            if (rootMethod.getReturnType() == ModelPart.class) {
                rootNode = (ModelPart) rootMethod.invoke(model);
            }
        } catch (Exception ignored) {}

        if (rootNode != null) {
            ModelPart found = searchNodeRecursively(rootNode, handKey, handedness + "arm", handedness + "Arm");
            if (found != null) return found;

            found = searchNodeRecursively(rootNode, legKey, handedness + "frontleg", handedness + "FrontLeg", handedness + "_leg", handedness + "leg", handedness + "Leg");
            if (found != null) return found;

            found = searchNodeRecursively(rootNode, wingKey, handedness + "wing", handedness + "Wing");
            if (found != null) return found;
        }

        // Fallback for non-hierarchical or custom mods using flat reflection:
        Class<?> current = model.getClass();
        while (current != Object.class && current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (ModelPart.class.isAssignableFrom(f.getType())) {
                    String name = f.getName().toLowerCase(Locale.ROOT);
                    if (name.contains(handedness)) {
                        if (name.contains("arm") || name.contains("leg") || name.contains("wing")) {
                            try {
                                f.setAccessible(true);
                                return (ModelPart) f.get(model);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private ModelPart searchNodeRecursively(ModelPart parent, String... possibleKeys) {
        for (String key : possibleKeys) {
            try {
                if (parent.hasChild(key)) {
                    return parent.getChild(key);
                }
            } catch (Exception ignored) {} 
        }
        
        // Deep Recurse: ModelPart contains an internal Map of children!
        try {
            for (Field f : ModelPart.class.getDeclaredFields()) {
                if (java.util.Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) f.get(parent);
                    if (map != null) {
                        for (Object val : map.values()) {
                            if (val instanceof ModelPart) {
                                ModelPart found = searchNodeRecursively((ModelPart) val, possibleKeys);
                                if (found != null) return found;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        
        return null;
    }

    @Override
    public PoseStack[] getPlacementCorrectors(HumanoidArm arm) {
        PartData[] targetParts = arm == HumanoidArm.LEFT ? leftHandParts : rightHandParts;
        int size = targetParts != null ? targetParts.length : 1; 
        PoseStack[] correctors = new PoseStack[size];
        for (int i = 0; i < size; i++) {
            PoseStack stack = new PoseStack();
            if (targetParts != null && targetParts.length > i && targetParts[i] != null && targetParts[i].placementCorrectors != null) {
                for (CorrectorData c : targetParts[i].placementCorrectors) {
                    if (c == null) continue;
                    float amt = c.amount;
                    if ("translate".equals(c.type)) {
                        stack.translate("x".equals(c.axis) ? amt : 0, "y".equals(c.axis) ? amt : 0, "z".equals(c.axis) ? amt : 0);
                    } else if ("rotate".equals(c.type)) {
                        float rad = (float) Math.toRadians(amt); // GSON parses degrees, Matrix4f uses radians usually or axis-angle
                        if ("x".equals(c.axis)) stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(amt));
                        else if ("y".equals(c.axis)) stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(amt));
                        else if ("z".equals(c.axis)) stack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(amt));
                    } else if ("scale".equals(c.type)) {
                        stack.scale("x".equals(c.axis) ? amt : 1, "y".equals(c.axis) ? amt : 1, "z".equals(c.axis) ? amt : 1);
                    }
                }
            }
            correctors[i] = stack;
        }
        return correctors;
    }

    @Override
    public Class<? extends EntityModel<?>> getModelClass() {
        return modelClass;
    }
}
