package me.ichun.mods.ichunutil.client.model.util;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.ichunutil.client.model.TabulaModelRenderer;
import me.ichun.mods.ichunutil.common.module.tabula.project.Identifiable;
import me.ichun.mods.ichunutil.common.module.tabula.project.Project;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class ModelHelper
{
    public static TabulaModelRenderer createModelPart(Project.Part part)
    {
        return createModelPart(part, false);
    }

    public static TabulaModelRenderer createModelPart(Project.Part part, boolean processChildren)
    {
        TabulaModelRenderer modelPart = new TabulaModelRenderer(part.texWidth, part.texHeight, part.texOffX, part.texOffY);

        modelPart.rotationPointX = part.rotPX;
        modelPart.rotationPointY = part.rotPY;
        modelPart.rotationPointZ = part.rotPZ;

        modelPart.rotateAngleX = (float)Math.toRadians(part.rotAX);
        modelPart.rotateAngleY = (float)Math.toRadians(part.rotAY);
        modelPart.rotateAngleZ = (float)Math.toRadians(part.rotAZ);

        modelPart.mirror = part.mirror;
        modelPart.showModel = part.showModel;

        part.boxes.forEach(box -> {
            int texOffX = modelPart.textureOffsetX;
            int texOffY = modelPart.textureOffsetY;
            modelPart.setTextureOffset(modelPart.textureOffsetX + box.texOffX, modelPart.textureOffsetY + box.texOffY);
            modelPart.addBox(box.posX, box.posY, box.posZ, box.dimX, box.dimY, box.dimZ, box.expandX, box.expandY, box.expandZ);
            modelPart.setTextureOffset(texOffX, texOffY);
        });

        if(processChildren)
        {
            part.children.forEach(child -> modelPart.addChild(createModelPart(child, true)));
        }
        return modelPart;
    }

    public static Project.Part createPartFor(ModelPart renderer, boolean processChildren)
    {
        if(renderer == null)
        {
            Project.Part part = new Project.Part(null, 0);
            part.boxes.clear();
            return part;
        }
        HashMap<ModelPart, Identifiable<?>> store = new HashMap<>();
        createPartFor("", renderer, store, null, processChildren);
        return (Project.Part)store.get(renderer);
    }

    public static void createPartFor(String name, ModelPart renderer, HashMap<ModelPart, Identifiable<?>> done, Identifiable<?> parent, boolean processChildren)
    {
        if(done.containsKey(renderer))
        {
            return;
        }
        Project.Part part = new Project.Part(parent, done.size());

        part.boxes.clear();
        part.name = name;
        
        part.texWidth = 64;
        part.texHeight = 32;
        
        part.rotPX = renderer.x;
        part.rotPY = renderer.y;
        part.rotPZ = renderer.z;

        part.rotAX = (float)Math.toDegrees(renderer.xRot);
        part.rotAY = (float)Math.toDegrees(renderer.yRot);
        part.rotAZ = (float)Math.toDegrees(renderer.zRot);

        part.mirror = false;
        part.showModel = renderer.visible;

        try {
            java.lang.reflect.Field cubesField = ModelPart.class.getDeclaredField("cubes");
            cubesField.setAccessible(true);
            java.util.List<?> cubes = (java.util.List<?>)cubesField.get(renderer);
            
            for(Object cubeObj : cubes)
            {
                ModelPart.Cube cube = (ModelPart.Cube)cubeObj;
                Project.Part.Box box = new Project.Part.Box(part);
                
                // Reflection to get min/max fields as they might be obfuscated in some environments or private
                box.posX = getFieldValue(cube, "minX", "f_104434_");
                box.posY = getFieldValue(cube, "minY", "f_104435_");
                box.posZ = getFieldValue(cube, "minZ", "f_104436_");
                float maxX = getFieldValue(cube, "maxX", "f_104437_");
                float maxY = getFieldValue(cube, "maxY", "f_104438_");
                float maxZ = getFieldValue(cube, "maxZ", "f_104439_");
                
                box.dimX = maxX - box.posX;
                box.dimY = maxY - box.posY;
                box.dimZ = maxZ - box.posZ;
                
                part.boxes.add(box);
            }
        } catch (Exception e) {
            // Fallback: add a simple box if extraction fails
            if(part.boxes.isEmpty()) {
                part.boxes.add(new Project.Part.Box(part));
            }
        }

        if(processChildren)
        {
            try {
                java.lang.reflect.Field field = null;
                try {
                    field = ModelPart.class.getDeclaredField("children");
                } catch (NoSuchFieldException e) {
                    field = ModelPart.class.getDeclaredField("f_104210_");
                }
                field.setAccessible(true);
                Map<String, ModelPart> children = (Map<String, ModelPart>)field.get(renderer);
                children.forEach((n, child) -> createPartFor(n, child, done, part, true));
            } catch (Exception e) {
                // Fallback
            }
        }

        if(parent instanceof Project)
        {
            ((Project)parent).parts.add(part);
        }
        else if(parent instanceof Project.Part)
        {
            ((Project.Part)parent).children.add(part);
        }
        done.put(renderer, part);
    }

    private static float getFieldValue(Object obj, String fieldName, String mcpName) {
        try {
            java.lang.reflect.Field f = null;
            try {
                f = obj.getClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                try {
                    f = obj.getClass().getDeclaredField(mcpName);
                } catch (NoSuchFieldException e2) {
                    // Try to find by index or iterate if names fail
                    for(java.lang.reflect.Field field : obj.getClass().getDeclaredFields()) {
                        if(field.getType() == float.class && field.getName().equals(fieldName)) {
                            f = field;
                            break;
                        }
                    }
                }
            }
            if(f != null) {
                f.setAccessible(true);
                return f.getFloat(obj);
            }
        } catch (Exception e) {}
        return 0;
    }

    public static Project.Part createPartFor(TabulaModelRenderer renderer, boolean processChildren)
    {
        if(renderer == null)
        {
            Project.Part part = new Project.Part(null, 0);
            part.boxes.clear();
            return part;
        }
        HashMap<TabulaModelRenderer, Identifiable<?>> store = new HashMap<>();
        createPartFor("", renderer, store, null, processChildren);
        return (Project.Part)store.get(renderer);
    }

    public static void createPartFor(String name, TabulaModelRenderer renderer, HashMap<TabulaModelRenderer, Identifiable<?>> done, Identifiable<?> parent, boolean processChildren)
    {
        if(done.containsKey(renderer))
        {
            return;
        }
        Project.Part part = new Project.Part(parent, done.size());

        part.boxes.clear();
        part.name = name;
        
        part.texWidth = Math.round(renderer.textureWidth);
        part.texHeight = Math.round(renderer.textureHeight);
        
        part.rotPX = renderer.rotationPointX;
        part.rotPY = renderer.rotationPointY;
        part.rotPZ = renderer.rotationPointZ;

        part.rotAX = (float)Math.toDegrees(renderer.rotateAngleX);
        part.rotAY = (float)Math.toDegrees(renderer.rotateAngleY);
        part.rotAZ = (float)Math.toDegrees(renderer.rotateAngleZ);

        part.mirror = renderer.mirror;
        part.showModel = renderer.showModel;

        if(processChildren)
        {
            renderer.childModels.forEach(child -> createPartFor("", child, done, part, true));
        }

        if(parent instanceof Project)
        {
            ((Project)parent).parts.add(part);
        }
        else if(parent instanceof Project.Part)
        {
            ((Project.Part)parent).children.add(part);
        }
        done.put(renderer, part);
    }

    public static void matchBoxesCount(Project.Part main, Project.Part target)
    {
        while(main.boxes.size() < target.boxes.size())
        {
            Project.Part.Box box = new Project.Part.Box(main);
            box.posX = 0; box.posY = 0; box.posZ = 0;
            box.dimX = 0; box.dimY = 0; box.dimZ = 0;
            box.expandX = 0; box.expandY = 0; box.expandZ = 0;
            box.texOffX = 0; box.texOffY = 0;
            main.boxes.add(box);
        }
    }
    
    public static Project.Part createInterimPart(Project.Part prevPart, Project.Part nextPart, float prog)
    {
        Project.Part part = new Project.Part(null, 0);
        part.boxes.clear();

        part.texWidth = Math.round(prevPart.texWidth + (nextPart.texWidth - prevPart.texWidth) * prog);
        part.texHeight = Math.round(prevPart.texHeight + (nextPart.texHeight - prevPart.texHeight) * prog);

        part.texOffX = Math.round(prevPart.texOffX + (nextPart.texOffX - prevPart.texOffX) * prog);
        part.texOffY = Math.round(prevPart.texOffY + (nextPart.texOffY - prevPart.texOffY) * prog);

        part.mirror = nextPart.mirror;

        part.rotPX = prevPart.rotPX + (nextPart.rotPX - prevPart.rotPX) * prog;
        part.rotPY = prevPart.rotPY + (nextPart.rotPY - prevPart.rotPY) * prog;
        part.rotPZ = prevPart.rotPZ + (nextPart.rotPZ - prevPart.rotPZ) * prog;

        part.rotAX = prevPart.rotAX + (nextPart.rotAX - prevPart.rotAX) * prog;
        part.rotAY = prevPart.rotAY + (nextPart.rotAY - prevPart.rotAY) * prog;
        part.rotAZ = prevPart.rotAZ + (nextPart.rotAZ - prevPart.rotAZ) * prog;

        for(int i = 0; i < Math.min(prevPart.boxes.size(), nextPart.boxes.size()); i++)
        {
            Project.Part.Box box = new Project.Part.Box(part);

            Project.Part.Box prevBox = prevPart.boxes.get(i);
            Project.Part.Box nextBox = nextPart.boxes.get(i);

            box.posX = prevBox.posX + (nextBox.posX - prevBox.posX) * prog;
            box.posY = prevBox.posY + (nextBox.posY - prevBox.posY) * prog;
            box.posZ = prevBox.posZ + (nextBox.posZ - prevBox.posZ) * prog;

            box.dimX = prevBox.dimX + (nextBox.dimX - prevBox.dimX) * prog;
            box.dimY = prevBox.dimY + (nextBox.dimY - prevBox.dimY) * prog;
            box.dimZ = prevBox.dimZ + (nextBox.dimZ - prevBox.dimZ) * prog;

            box.expandX = prevBox.expandX + (nextBox.expandX - prevBox.expandX) * prog;
            box.expandY = prevBox.expandY + (nextBox.expandY - prevBox.expandY) * prog;
            box.expandZ = prevBox.expandZ + (nextBox.expandZ - prevBox.expandZ) * prog;

            box.texOffX = Math.round(prevBox.texOffX + (nextBox.texOffX - prevBox.texOffX) * prog);
            box.texOffY = Math.round(prevBox.texOffY + (nextBox.texOffY - prevBox.texOffY) * prog);

            part.boxes.add(box);
        }

        for(int i = 0; i < Math.min(prevPart.children.size(), nextPart.children.size()); i++)
        {
            part.children.add(createInterimPart(prevPart.children.get(i), nextPart.children.get(i), prog));
        }

        return part;
    }
}
