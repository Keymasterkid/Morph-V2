package me.ichun.mods.ichunutil.common.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import me.ichun.mods.ichunutil.common.iChunUtil;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;

public class ResourceReloadListener<T> extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>>
{
    private static final Gson DEFAULT_GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

    private final Class<T> classType;
    private final String folder;

    private final Gson parser;
    private T defaultObj = null;
    public HashMap<ResourceLocation, T> objects = new HashMap<>();

    public ResourceReloadListener(String resourceFolder, Class<T> classType)
    {
        this(DEFAULT_GSON, resourceFolder, classType);
    }

    public ResourceReloadListener(Gson gsonParser, String resourceFolder, Class<T> classType)
    {
        this.folder = resourceFolder;
        this.classType = classType;
        this.parser = gsonParser;

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
    }

    public <K extends ResourceReloadListener<T>> K setDefault(T defaultObj)
    {
        this.defaultObj = defaultObj;
        return (K)this;
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> map = new HashMap<>();
        for (ResourceLocation resourcelocation : resourceManager.listResources(this.folder, (p_223405_) -> p_223405_.getPath().endsWith(".json")).keySet()) {
            try {
                try (java.io.InputStream inputstream = resourceManager.open(resourcelocation);
                     java.io.Reader reader = new java.io.InputStreamReader(inputstream, java.nio.charset.StandardCharsets.UTF_8)) {
                    JsonElement jsonelement = GsonHelper.fromJson(this.parser, reader, JsonElement.class);
                    if (jsonelement != null) {
                        map.put(resourcelocation, jsonelement);
                    }
                }
            } catch (Exception exception) {
                iChunUtil.LOGGER.error("Couldn't parse data file {} from {}", resourcelocation, resourcelocation, exception);
            }
        }
        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager iResourceManager, ProfilerFiller iProfiler)
    {
        json.forEach((k, v) -> {
            try
            {
                objects.put(k, parser.fromJson(v, classType));
            }
            catch(Exception e)
            {
                iChunUtil.LOGGER.warn("Error parsing resource : {}", k);
            }
        });
    }

    public @Nullable T get(ResourceLocation key)
    {
        return objects.containsKey(key) ? objects.get(key) : defaultObj;
    }

    private void onAddReloadListener(AddClientReloadListenersEvent event)
    {
        event.addListener(ResourceLocation.fromNamespaceAndPath("ichunutil", folder), this);
    }
}
