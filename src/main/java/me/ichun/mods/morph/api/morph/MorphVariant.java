package me.ichun.mods.morph.api.morph;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import me.ichun.mods.morph.api.MorphApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayer;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class MorphVariant implements Comparable<MorphVariant>
{
    public static final int IDENTIFIER_LENGTH = 20;
    public static final String IDENTIFIER_DEFAULT_PLAYER_STATE = "default_player_state";
    public static final String NBT_PLAYER_ID = "Morph_Player_ID";
    public static String[] TAGS_TO_TAKE = new String[] { "CustomName", "CustomNameVisible", "ForgeCaps", "ForgeData" }; //Intentionally non-final. If you're going to be adding to this please remember to include tthe originals!

    @Nonnull
    public ResourceLocation id; // the ID of the morph
    @Nonnull
    public CompoundTag nbtMorph; //special morph specific NBT
    public CompoundTag nbtCommon; //common nbt tags shared by all variants
    public ArrayList<Variant> variants; //if populated, thisVariant should not be used.

    public Variant thisVariant; //this is set for a specific variant/render. variants should be left empty.

    public MorphVariant(ResourceLocation id)
    {
        this.id = id;
        this.nbtMorph = new CompoundTag();
        this.variants = new ArrayList<>();
    }

    private MorphVariant()
    {
        this.variants = new ArrayList<>();
    }

    public void setLiving(CompoundTag tag) //Not used for PLAYERS
    {
        nbtCommon = tag;
    }

    public void writeSupportedAttributes(LivingEntity living)
    {
        Map<ResourceLocation, AttributeConfig> attrs = MorphApi.getApiImpl().getSupportedAttributes();
        for(Map.Entry<ResourceLocation, AttributeConfig> e : attrs.entrySet())
        {
            net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(e.getKey()).ifPresent(holder -> {
                if(living.getAttributes().hasAttribute(holder))
                {
                    AttributeConfig attributeConfig = e.getValue();
                    double value = living.getAttributeValue(holder);
                    if(attributeConfig.moreIsBetter) //more is better
                    {
                        if(attributeConfig.cap != null && value > attributeConfig.cap)
                        {
                            value = attributeConfig.cap;
                        }
                    }
                    else //less is better
                    {
                        if(attributeConfig.cap != null && value < attributeConfig.cap)
                        {
                            value = attributeConfig.cap;
                        }
                    }

                    nbtMorph.putDouble("attr_" + e.getKey().toString(), value);
                }
            });
        }
    }

    public static void writeDefaults(LivingEntity living, CompoundTag tag) //taken from Entity.saveWithoutId
    {
        CompoundTag defs = new CompoundTag();

        living.saveWithoutId(defs); //because I can't copy out serialiseCaps

        for(String s : TAGS_TO_TAKE)
        {
            if(defs.contains(s))
            {
                tag.put(s, defs.get(s));
            }
        }
    }

    public boolean hasVariants()
    {
        return !variants.isEmpty();
    }

    public boolean combineVariants(MorphVariant variant)
    {
        if(!isSameMorphType(variant))
        {
            return false;
        }

        //special InteractionHandling for players
        if(id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)))
        {
            variants.add(variant.thisVariant);
            return true;
        }

        //Compare the tags for living entities.
        addBetterMorphData(variant.nbtMorph);

        CompoundTag variantTag = variant.getCumulativeTags();

        //compare with our commons first to see what doesn't match.
        HashSet<String> uncommons = new HashSet<>();
        for(String key : nbtCommon.getAllKeys())
        {
            Tag varNBT = variantTag.get(key);

            if(varNBT == null || !varNBT.equals(nbtCommon.get(key))) //uncommon, mark for cloning in all the other variants
            {
                uncommons.add(key);
            }
            else //common value, remove it from their variants.
            {
                variantTag.remove(key);
            }
        }

        //add the now uncommon to the existing variants, and remove the previous common, it's not common anymore.
        for(String key : uncommons)
        {
            Tag nbt = nbtCommon.get(key);
            for(Variant aVariant : variants)
            {
                aVariant.nbtVariant.put(key, nbt.copy());
            }
            nbtCommon.remove(key);
        }

        //the commons have been stripped so what remains is the variant.
        variant.thisVariant.nbtVariant = variantTag;

        variants.add(variant.thisVariant);

        //we've fixed the uncommons... now get the new commons.
        gatherNewCommons();

        return true;
    }

    public boolean removeVariant(Variant variant)
    {
        boolean flag = false;
        for(int i = variants.size() - 1; i >= 0; i--)
        {
            if(variants.get(i).identifier.equals(variant.identifier))
            {
                variants.remove(i);
                flag = true;
            }
        }

        if(flag && !id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER))) //player morphs don't have commons
        {
            if(variants.size() >= 2)
            {
                gatherNewCommons();
            }
            else if(!variants.isEmpty()) //Only one variant left
            {
                variants.get(0).nbtVariant.merge(nbtCommon);
                nbtCommon = new CompoundTag(); // no more commons
            }
            else //no more variants, aka no more common tags.
            {
                nbtCommon = new CompoundTag();
            }
        }

        return flag;
    }

    public Variant getVariantById(String id)
    {
        for(Variant variant : variants)
        {
            if(variant.identifier.equals(id))
            {
                return variant;
            }
        }

        if(thisVariant != null && thisVariant.identifier.equals(id))
        {
            return thisVariant;
        }

        return null;
    }

    public void gatherNewCommons()
    {
        CompoundTag commons = new CompoundTag();

        //add all the tags we know of first
        if (!variants.isEmpty()) {
            commons.merge(variants.get(0).nbtVariant);
        }

        //now we compare
        for(String key : new ArrayList<>(commons.getAllKeys())) { // Iterate over a copy to allow modification
            for(Variant variant : variants)
            {
                if(!variant.nbtVariant.contains(key) || !commons.get(key).equals(variant.nbtVariant.get(key)))
                {
                    commons.remove(key);
                    break; // This key is not common, move to the next key
                }
            }
        }

        //remove from the variants
        nbtCommon.merge(commons);
        for(String s : commons.getAllKeys())
        {
            for(Variant variant : variants)
            {
                variant.nbtVariant.remove(s);
            }
        }
    }

    public boolean containsVariant(MorphVariant variant)
    {
        //special InteractionHandling for players
        if(id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)))
        {
            for(Variant aVariant : variants)
            {
                if(aVariant.playerUUID.equals(variant.thisVariant.playerUUID))
                {
                    return true;
                }
            }
        }
        else if(!variant.id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)))
        {
            CompoundTag variantTags = variant.getCumulativeTags();

            for(Variant aVariant : variants)
            {
                CompoundTag aVariantTags = getCumulativeTagsWithVariant(aVariant);

                if(variantTags.equals(aVariantTags))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isSameMorphType(MorphVariant variant)
    {
        return id.equals(variant.id);
    }

    private boolean addBetterMorphData(CompoundTag tag) //returns true when the data is better.
    {
        boolean flag = false;

        Map<ResourceLocation, AttributeConfig> supportedAttributes = MorphApi.getApiImpl().getSupportedAttributes();

        for(String key : nbtMorph.getAllKeys())
        {
            Tag e = nbtMorph.get(key);
            if(key.startsWith("attr_")) //it's an attribute key
            {
                ResourceLocation id = ResourceLocation.parse(key.substring(5));
                if(supportedAttributes.containsKey(id))
                {
                    AttributeConfig attributeConfig = supportedAttributes.get(id);
                    final double value = tag.getDouble(key);
                    if(attributeConfig.moreIsBetter) //more is better
                    {
                        if(nbtMorph.getDouble(key) < value)
                        {
                            nbtMorph.putDouble(key, value);

                            if(attributeConfig.cap != null && value > attributeConfig.cap)
                            {
                                nbtMorph.putDouble(key, attributeConfig.cap);
                            }
                            flag = true;
                        }
                    }
                    else //less is better
                    {
                        if(nbtMorph.getDouble(key) > value)
                        {
                            nbtMorph.putDouble(key, value);

                            if(attributeConfig.cap != null && value < attributeConfig.cap)
                            {
                                nbtMorph.putDouble(key, attributeConfig.cap);
                            }
                            flag = true;
                        }
                    }
                }
            }
        }

        for(String key : tag.getAllKeys())
        {
            Tag e = tag.get(key);
            if(id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)))
            {
                nbtMorph.put(key, e);
                flag = true;
            }
        }
        return flag;
    }

    public boolean hasFavourite()
    {
        for(Variant variant : variants)
        {
            if(variant.isFavourite)
            {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Deprecated
    //remove in 1.18
    public LivingEntity createEntityInstance(Level world, @Nullable UUID playerId)
    {
        return createEntityInstance(world, playerId != null ? world.getPlayerByUUID(playerId) : (Player) null);
    }

    @Nonnull
    public LivingEntity createEntityInstance(Level world, @Nullable Player player)
    {
        LivingEntity entInstance = null;
        EntityType<?> value = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id);
        if(value != null)
        {
            try
            {
                if(value.equals(EntityType.PLAYER))
                {
                    entInstance = world.isClientSide ? createPlayer(world, thisVariant.playerUUID) : new FakePlayer((ServerLevel)world, MorphApi.getApiImpl().getGameProfile(thisVariant.playerUUID, null));

                    if(player != null)
                    {
                        entInstance.getPersistentData().merge(player.getPersistentData());
                    }

                    //DO NOT Use NBT modifiers, they will strip everything anyway. We're copying the tags out for appearance. (I hope this doesn't bite me in the behind)

                    //Allow NBT Modifiers to "clean" the persistent data we copied over
                    //                    NbtModifier nbtModifier = NbtHandler.getModifierFor(entInstance);
                    //                    nbtModifier.apply(entInstance.getPersistentData());
                    //
                    //                    NbtHandler.removeEmptyCompoundTags(entInstance.getPersistentData());

                    for(BiConsumer<LivingEntity, CompoundTag> consumer : MorphApi.getApiImpl().getVariantNbtTagReaders())
                    {
                        consumer.accept(entInstance, entInstance.getPersistentData());
                    }
                }
                else
                {
                    CompoundTag tags = getCumulativeTags();
                    Entity ent = value.create(world);
                    if(ent instanceof LivingEntity)
                    {
                        ent.load(tags);

                        entInstance = (LivingEntity)ent;

                        for(BiConsumer<LivingEntity, CompoundTag> consumer : MorphApi.getApiImpl().getVariantNbtTagReaders())
                        {
                            consumer.accept(entInstance, tags);
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                MorphApi.getLogger().error("Error creating Morph entity for ID: {}", id);
                t.printStackTrace();
            }
        }

        if(entInstance == null) //we can't find the entity type or errored out somewhere... have a pig.
        {
            MorphApi.getLogger().error("Cannot find entity type {} have a pig instead!", id);
            entInstance = EntityType.PIG.create(world);
            entInstance.setCustomName(Component.literal("Invalid Morph Pig"));
        }

        entInstance.setId(MorphInfo.getNextEntId()); //to prevent ID collision

        if(player != null)
        {
            entInstance.getPersistentData().putUUID(NBT_PLAYER_ID, player.getGameProfile().getId());
        }

        return entInstance;
    }

    @OnlyIn(Dist.CLIENT)
    private Player createPlayer(Level world, UUID uuid)
    {
        Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        GameProfile gameProfile = MorphApi.getApiImpl().getGameProfile(uuid, null);
        boolean added = false;
        if(mc.getConnection().getPlayerInfo(gameProfile.getId()) == null) //we have to assign a PlayerInfo for the player skin to render.
        {
            try {
                java.lang.reflect.Field field = net.minecraft.client.multiplayer.ClientPacketListener.class.getDeclaredField("playerInfoMap");
                field.setAccessible(true);
                Map<UUID, PlayerInfo> map = (Map<UUID, PlayerInfo>) field.get(mc.getConnection());
                map.put(gameProfile.getId(), new PlayerInfo(gameProfile, false));
                added = true;
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

        RemotePlayer player = new RemotePlayer((ClientLevel)world, gameProfile);
        try {
            java.lang.reflect.Field field = Player.class.getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION");
            field.setAccessible(true);
            net.minecraft.network.syncher.EntityDataAccessor<Byte> accessor = (net.minecraft.network.syncher.EntityDataAccessor<Byte>) field.get(null);
            player.getEntityData().set(accessor, (byte)127); //All model parts shown
        } catch (Exception e) {
            // e.printStackTrace();
        }

        if(added)
        {
            try {
                java.lang.reflect.Field field = net.minecraft.client.multiplayer.ClientPacketListener.class.getDeclaredField("playerInfoMap");
                field.setAccessible(true);
                Map<UUID, PlayerInfo> map = (Map<UUID, PlayerInfo>) field.get(mc.getConnection());
                map.remove(gameProfile.getId());
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

        return player;
    }

    public MorphVariant getAsVariant(Variant variant)
    {
        MorphVariant morph = createFromNBT(write(new CompoundTag()));
        morph.variants.clear();
        morph.thisVariant = variant;

        return morph;
    }

    public CompoundTag getCumulativeTags()
    {
        return getCumulativeTagsWithVariant(thisVariant);
    }

    public CompoundTag getCumulativeTagsWithVariant(Variant variant)
    {
        CompoundTag tags = new CompoundTag();

        tags.merge(nbtCommon);
        tags.merge(variant.nbtVariant);

        return tags;
    }

    public CompoundTag write(CompoundTag tag)
    {
        tag.putString("id", id.toString());
        tag.put("nbtMorph", nbtMorph);
        if(!id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)))
        {
            tag.put("nbtCommon", nbtCommon);
        }

        tag.putInt("variantCount", variants.size());
        for(int i = 0; i < variants.size(); i++)
        {
            tag.put("variant_" + i, variants.get(i).write(new CompoundTag()));
        }

        if(thisVariant != null)
        {
            tag.put("thisVariant", thisVariant.write(new CompoundTag()));
        }
        return tag;
    }

    public void read(CompoundTag tag)
    {
        id = ResourceLocation.parse(tag.getString("id"));
        nbtMorph = tag.getCompound("nbtMorph");
        if(!id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)))
        {
            nbtCommon = tag.getCompound("nbtCommon");
        }

        variants.clear();
        int count = tag.getInt("variantCount");
        for(int i = 0; i < count; i++)
        {
            Variant variant = new Variant();
            variant.read(tag.getCompound("variant_" + i));
            variants.add(variant);
        }

        if(tag.contains("thisVariant"))
        {
            Variant variant = new Variant();
            variant.read(tag.getCompound("thisVariant"));
            thisVariant = variant;
        }
    }

    @Override
    public boolean equals(Object obj) //only used for a single variant
    {
        if(obj instanceof MorphVariant)
        {
            MorphVariant variant = (MorphVariant)obj;

            if(id.equals(variant.id) && thisVariant != null && variant.thisVariant != null)
            {
                if(id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)))
                {
                    return thisVariant.playerUUID.equals(variant.thisVariant.playerUUID);
                }
                else
                {
                    return getCumulativeTags().equals(variant.getCumulativeTags());
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(MorphVariant o)
    {
        if(id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)) && !id.equals(o.id)) //this is a player morph. always first
        {
            return -1; //we're before...
        }
        else if(o.id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)) && !id.equals(o.id))
        {
            return 1;
        }

        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id);
        EntityType<?> otherType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(o.id);
        if(type != null)
        {
            if(otherType != null)
            {
                if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
                {
                    return I18n.get(type.getDescriptionId()).compareTo(I18n.get(otherType.getDescriptionId()));
                }
                return type.getDescriptionId().compareTo(otherType.getDescriptionId());
            }
            return -1; //we have a type, we're before
        }
        else
        {
            if(otherType == null)
            {
                return 0; //they also don't have a type, no comparator
            }
            return 1;//we don't have a type
        }
    }

    public static MorphVariant createFromNBT(CompoundTag tag)
    {
        MorphVariant variant = new MorphVariant();
        variant.read(tag);
        return variant;
    }

    public static MorphVariant createPlayerMorph(@Nonnull UUID owner, boolean isVariant) //creates the base morph + variant of the player.
    {
        MorphVariant variant = new MorphVariant(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER));
        Variant var = new Variant();
        var.playerUUID = owner;
        if(isVariant)
        {
            variant.thisVariant = var;
        }
        else
        {
            variant.variants.add(var);
        }

        return variant;
    }

    @Override
    public int hashCode()
    {
        return thisVariant != null ? thisVariant.hashCode() : super.hashCode();
    }

    public static class Variant
    {
        public String identifier;
        public UUID playerUUID; // for player morphs
        public CompoundTag nbtVariant;
        public boolean isFavourite;

        public Variant()
        {
            this.identifier = RandomStringUtils.randomAscii(IDENTIFIER_LENGTH);
            this.nbtVariant = new CompoundTag();
            this.isFavourite = false;
        }

        public CompoundTag write(CompoundTag tag)
        {
            tag.putString("identifier", identifier);
            if(playerUUID != null)
            {
                tag.putUUID("playerUUID", playerUUID);
            }
            else
            {
                tag.put("nbtVariant", nbtVariant);
            }
            tag.putBoolean("isFavourite", isFavourite);
            return tag;
        }

        public void read(CompoundTag tag)
        {
            identifier = tag.getString("identifier");
            if(tag.contains("playerUUID"))
            {
                playerUUID = tag.getUUID("playerUUID");
            }
            else
            {
                nbtVariant = tag.getCompound("nbtVariant");
            }
            isFavourite = tag.getBoolean("isFavourite");
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj instanceof Variant)
            {
                return playerUUID != null ? playerUUID.equals(((Variant)obj).playerUUID) : nbtVariant.equals(((Variant)obj).nbtVariant);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return identifier.hashCode();
        }
    }
}
