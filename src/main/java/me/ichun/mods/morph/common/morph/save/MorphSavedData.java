package me.ichun.mods.morph.common.morph.save;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.util.datafix.DataFixTypes;
import com.mojang.serialization.Codec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MorphSavedData extends SavedData
{
    public static final String ID = "morph_save";
    public static final Codec<MorphSavedData> CODEC = CompoundTag.CODEC.xmap(
            tag -> load(tag, null),
            data -> data.save(new CompoundTag(), null)
    );
    public static final SavedDataType<MorphSavedData> TYPE = new SavedDataType<>(
            ID,
            MorphSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );
    public HashMap<UUID, PlayerMorphData> playerMorphs = new HashMap<>();

    public MorphSavedData()
    {
    }

    public static MorphSavedData load(CompoundTag tag, HolderLookup.Provider provider)
    {
        MorphSavedData data = new MorphSavedData();
        int count = tag.getInt("count").orElse(0);
        for(int i = 0; i < count; i++)
        {
            PlayerMorphData playerData = new PlayerMorphData();
            playerData.read(tag.getCompound("morph_" + i).orElse(new net.minecraft.nbt.CompoundTag()));

            data.playerMorphs.put(playerData.owner, playerData);
        }
        return data;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        tag.putInt("count", playerMorphs.size());

        int i = 0;
        for(Map.Entry<UUID, PlayerMorphData> entry : playerMorphs.entrySet())
        {
            tag.put("morph_" + i, entry.getValue().write(new CompoundTag()));
            i++;
        }

        return tag;
    }
}
