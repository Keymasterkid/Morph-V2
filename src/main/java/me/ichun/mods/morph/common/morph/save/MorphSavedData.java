package me.ichun.mods.morph.common.morph.save;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MorphSavedData extends SavedData
{
    public static final String ID = "morph_save";
    public HashMap<UUID, PlayerMorphData> playerMorphs = new HashMap<>();

    public MorphSavedData()
    {
    }

    public static MorphSavedData load(CompoundTag tag, HolderLookup.Provider provider)
    {
        MorphSavedData data = new MorphSavedData();
        int count = tag.getInt("count");
        for(int i = 0; i < count; i++)
        {
            PlayerMorphData playerData = new PlayerMorphData();
            playerData.read(tag.getCompound("morph_" + i));

            data.playerMorphs.put(playerData.owner, playerData);
        }
        return data;
    }

    @Override
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
