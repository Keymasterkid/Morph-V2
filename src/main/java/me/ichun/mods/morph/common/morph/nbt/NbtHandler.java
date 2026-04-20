package me.ichun.mods.morph.common.morph.nbt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.ichun.mods.ichunutil.common.util.IOUtil;
import me.ichun.mods.morph.api.event.MorphLoadResourceEvent;
import me.ichun.mods.morph.api.mob.nbt.NbtModifier;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.resource.ResourceHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class NbtHandler
{
    public static final HashMap<Class<? extends LivingEntity>, NbtModifier> NBT_MODIFIERS = new HashMap<>();
    public static final HashMap<Class<?>, NbtModifier> NBT_MODIFIERS_INTERFACES = new HashMap<>();

    private enum NbtJsonLoadResult
    {
        SUCCESS,
        MISSING_FOR_CLASS,
        UNRESOLVED_CLASS
    }

    public static void loadNbtModifiers()
    {
        NBT_MODIFIERS.clear();
        NBT_MODIFIERS_INTERFACES.clear();

        AtomicInteger skippedUnresolved = new AtomicInteger(0);

        //        serialiseModifiers();

        try
        {
            IOUtil.scourDirectoryForFiles(ResourceHandler.getMorphDir().resolve("nbt"), p -> {
                if(p.getFileName().toString().endsWith(".json"))
                {
                    File file = p.toFile();
                    try
                    {
                        String json = FileUtils.readFileToString(file, "UTF-8");
                        NbtJsonLoadResult result = readNbtJson(json, file.toPath(), skippedUnresolved);
                        if(result == NbtJsonLoadResult.SUCCESS)
                        {
                            return true;
                        }
                        if(result == NbtJsonLoadResult.MISSING_FOR_CLASS)
                        {
                            Morph.LOGGER.error("Error reading NBT Modifier file, missing or empty forClass: {}", file);
                            return false;
                        }
                        // UNRESOLVED_CLASS: optional mod / wrong classpath; already counted and logged at DEBUG in readNbtJson
                        return false;
                    }
                    catch(IOException | JsonSyntaxException | IllegalStateException e)
                    {
                        Morph.LOGGER.error("Error reading NBT Modifier file: {}", file);
                        e.printStackTrace();
                    }
                }
                return false;
            });
        }
        catch(IOException e)
        {
            Morph.LOGGER.error("Error loading NBT Modifier files.", e);
        }

        int skipped = skippedUnresolved.get();
        if(skipped > 0)
        {
            Morph.LOGGER.info("Skipped {} NBT modifier file(s) (forClass not on classpath)", skipped);
        }

        Morph.LOGGER.info("Loaded {} NBT Modifier(s)", NBT_MODIFIERS.size() + NBT_MODIFIERS_INTERFACES.size());

        setupInterfaceModifiers();

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new MorphLoadResourceEvent(MorphLoadResourceEvent.Type.NBT));
    }

    private static NbtJsonLoadResult readNbtJson(String json, Path sourcePath, AtomicInteger skippedUnresolved) throws JsonSyntaxException, IllegalStateException
    {
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(json).getAsJsonObject();
        if(jsonObject.has("forClass"))
        {
            String rawForClass = jsonObject.get("forClass").getAsString();
            if(rawForClass == null || rawForClass.isBlank())
            {
                return NbtJsonLoadResult.MISSING_FOR_CLASS;
            }
            String className = rawForClass;

            // 1.16 backwards compatibility mapping for MCP to Mojmap
            if (className.startsWith("net.minecraft.entity.")) {
                className = className.replace("net.minecraft.entity.", "net.minecraft.world.entity.");
                
                if (className.startsWith("net.minecraft.world.entity.passive.horse.")) {
                     className = className.replace("passive.horse.", "animal.horse.");
                } else if (className.startsWith("net.minecraft.world.entity.passive.fish.")) {
                     className = className.replace("passive.fish.", "animal.");
                } else if (className.startsWith("net.minecraft.world.entity.passive.")) {
                     className = className.replace("passive.", "animal.");
                } else if (className.startsWith("net.minecraft.world.entity.item.")) {
                     className = className.replace("item.", "decoration.");
                } else if (className.equals("net.minecraft.world.entity.merchant.villager.VillagerEntity")) {
                     className = "net.minecraft.world.entity.npc.Villager";
                }
                
                if (className.endsWith("Entity") && !className.equals("net.minecraft.world.entity.LivingEntity")) {
                     className = className.substring(0, className.length() - 6);
                }
                
                // Specific edge cases
                if (className.equals("net.minecraft.world.entity.animal.Tameable")) {
                     className = "net.minecraft.world.entity.TamableAnimal";
                } else if (className.equals("net.minecraft.world.entity.monster.Enderman")) {
                     className = "net.minecraft.world.entity.monster.EnderMan";
                } else if (className.equals("net.minecraft.world.entity.animal.Mooshroom")) {
                     className = "net.minecraft.world.entity.animal.MushroomCow";
                } else if (className.equals("net.minecraft.world.entity.animal.SnowGolem")) {
                     className = "net.minecraft.world.entity.animal.SnowGolem";
                }
            }

            Class clz = null;
            // Try multiple strategies to resolve the class name
            // Strategy 1: Direct Class.forName
            try {
                clz = Class.forName(className);
            } catch (ClassNotFoundException e1) {
                // Strategy 2: Try context classloader
                try {
                    clz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e2) {
                    // ignored, try next strategy
                }
            }

            // Strategy 3: In 1.21.8+, Mojang moved many entities into subpackages
            // e.g. animal.Sheep → animal.sheep.Sheep, monster.Creeper → monster.creeper.Creeper
            if (clz == null) {
                String simpleName = className.substring(className.lastIndexOf('.') + 1);
                String packageName = className.substring(0, className.lastIndexOf('.'));
                String subPackageName = packageName + "." + simpleName.toLowerCase(java.util.Locale.ROOT) + "." + simpleName;
                try {
                    clz = Class.forName(subPackageName);
                } catch (ClassNotFoundException e3) {
                    try {
                        clz = Class.forName(subPackageName, false, Thread.currentThread().getContextClassLoader());
                    } catch (ClassNotFoundException e4) {
                        // ignored, try next strategy
                    }
                }
            }

            // Strategy 4: Search entity registry by simple name
            if (clz == null) {
                String simpleName = className.substring(className.lastIndexOf('.') + 1);
                for (net.minecraft.world.entity.EntityType<?> type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
                    try {
                        // Try to get the class via the entity type's factory by checking known classes
                        Class<?> typeClass = type.getBaseClass();
                        if (typeClass != null && typeClass.getSimpleName().equals(simpleName)) {
                            clz = typeClass;
                            break;
                        }
                    } catch (Throwable t) {
                        // getBaseClass may not exist in this version, ignore
                    }
                }
            }

            if (clz == null) {
                skippedUnresolved.incrementAndGet();
                Morph.LOGGER.warn("NBT Modifier skipped (class not on classpath): {} forClass={} resolvedName={}",
                    sourcePath, rawForClass, className);
                return NbtJsonLoadResult.UNRESOLVED_CLASS;
            }

            boolean forInterface = jsonObject.has("isInterface") && jsonObject.get("isInterface").getAsBoolean();

            if((!forInterface && NBT_MODIFIERS.containsKey(clz)) || (forInterface && NBT_MODIFIERS_INTERFACES.containsKey(clz)))
            {
                Morph.LOGGER.warn("We already have another NBT Modifier for {}", clz.getName());
            }

            try
            {
                NbtModifier nbtModifier = ResourceHandler.GSON.fromJson(json, NbtModifier.class);
                if(forInterface)
                {
                    NBT_MODIFIERS_INTERFACES.put(clz, nbtModifier);
                }
                else
                {
                    NBT_MODIFIERS.put(clz, nbtModifier);
                }
            }
            catch(Throwable t)
            {
                Morph.LOGGER.error("Error deserialising NBT Modifier for {}", clz.getName());
                t.printStackTrace();
            }
            return NbtJsonLoadResult.SUCCESS;
        }
        return NbtJsonLoadResult.MISSING_FOR_CLASS;
    }

    private static void serialiseModifiers()
    {
        Path file = ResourceHandler.getMorphDir().resolve("nbt").resolve("LivingEntity.json");

        NbtModifier modifier = new NbtModifier();
        modifier.forClass = LivingEntity.class.getName();
        String[] strip = new String[] { "Health", "HurtTime", "HurtByTimestamp", "DeathTime", "AbsorptionAmount", "FallFlying", "SleepingX", "SleepingY", "SleepingZ", "Brain" };
        for(String s : strip)
        {
            NbtModifier.Modifier mod = new NbtModifier.Modifier();
            mod.key = s;
            mod.keep = true;
            modifier.modifiers.add(mod);
        }

        NbtModifier.Modifier hatsMod = new NbtModifier.Modifier();
        hatsMod.key = "ForgeCaps";

        NbtModifier.Modifier partMod = new NbtModifier.Modifier();
        partMod.key = "hats:capability_hat";
        partMod.keep = true;

        hatsMod.nestedModifiers = new ArrayList<>();
        hatsMod.nestedModifiers.add(partMod);

        ArrayList<NbtModifier.Modifier> list = new ArrayList<>();
        list.add(hatsMod);

        modifier.modSpecificModifiers.put("hats", list);

        try
        {
            String json = ResourceHandler.GSON.toJson(modifier);
            FileUtils.writeStringToFile(file.toFile(), json, "UTF-8");
        }
        catch(IOException ignored){}
        catch(Throwable e1)
        {
            e1.printStackTrace();
        }
    }

    public static NbtModifier getModifierFor(LivingEntity living)
    {
        NbtModifier modifier = getModifierFor(living.getClass());

        //we're about to use this modifier. Set up the modifier values
        modifier.setupValues();

        return modifier;
    }

    public static NbtModifier getModifierFor(Class clz)
    {
        NbtModifier modifier;
        boolean wasInMap = NBT_MODIFIERS.containsKey(clz);
        if(wasInMap)
        {
            modifier = NBT_MODIFIERS.get(clz);
            if(modifier.toKeep != null) // it's been set up;
            {
                return modifier;
            }
        }
        else
        {
            modifier = new NbtModifier();
            NBT_MODIFIERS.put(clz, modifier);
        }

        modifier.toKeep = new HashSet<>();
        modifier.keyToModifier = new HashMap<>();

        if(clz != LivingEntity.class)
        {
            //get the parent class's modifier and add their modifiers
            NbtModifier parentModifier = getModifierFor(clz.getSuperclass());

            modifier.toKeep.addAll(parentModifier.toKeep);
            modifier.keyToModifier.putAll(parentModifier.keyToModifier);
        }

        //Check the class' interfaces
        for(Map.Entry<Class<?>, NbtModifier> e : NBT_MODIFIERS_INTERFACES.entrySet())
        {
            if(e.getKey().isAssignableFrom(clz))
            {
                modifier.toKeep.addAll(e.getValue().toKeep);
                modifier.keyToModifier.putAll(e.getValue().keyToModifier);
            }
        }

        //setup adds this class' own modifiers.
        modifier.setup();

        return modifier;
    }

    private static void setupInterfaceModifiers()
    {
        for(Map.Entry<Class<?>, NbtModifier> e : NBT_MODIFIERS_INTERFACES.entrySet())
        {
            e.getValue().toKeep = new HashSet<>();
            e.getValue().keyToModifier = new HashMap<>();

            e.getValue().setup();
        }
    }

    public static void removeEmptyCompoundTags(CompoundTag tag)
    {
        java.util.List<String> toRemove = new java.util.ArrayList<>();
        tag.keySet().forEach(k -> {
            net.minecraft.nbt.Tag val = tag.get(k);
            if(val instanceof CompoundTag child) {
                removeEmptyCompoundTags(child);
                if(child.isEmpty()) toRemove.add(k);
            }
        });
        toRemove.forEach(tag::remove);
    }
}
