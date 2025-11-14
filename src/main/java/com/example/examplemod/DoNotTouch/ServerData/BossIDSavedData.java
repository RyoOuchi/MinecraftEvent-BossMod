package com.example.examplemod.DoNotTouch.ServerData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class BossIDSavedData extends SavedData {

    private final List<Integer> bossIds = new ArrayList<>();

    public List<Integer> getBossIds() {
        return bossIds;
    }

    public void addBossId(int id) {
        bossIds.add(id);
        setDirty();
    }

    public void removeBossId(int id) {
        bossIds.remove(Integer.valueOf(id));
        setDirty();
    }

    public void clearBossIds() {
        bossIds.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        int[] arr = bossIds.stream().mapToInt(Integer::intValue).toArray();
        tag.putIntArray("BossIDs", arr);
        return tag;
    }

    public static BossIDSavedData load(CompoundTag tag) {
        BossIDSavedData data = new BossIDSavedData();
        int[] arr = tag.getIntArray("BossIDs");
        for (int i : arr) data.bossIds.add(i);
        return data;
    }

    public static BossIDSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                BossIDSavedData::load,
                BossIDSavedData::new,
                "boss_ids"
        );
    }
}