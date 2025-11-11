package com.example.examplemod.ServerData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class ServerSavedData extends SavedData {
    private final Map<String, BlockPos> servers = new HashMap<>();

    public ServerSavedData() {}

    public static ServerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ServerSavedData::load, ServerSavedData::new, "server_data");
    }

    public static ServerSavedData load(CompoundTag tag) {
        ServerSavedData data = new ServerSavedData();
        ListTag list = tag.getList("servers", Tag.TAG_COMPOUND);

        for (Tag element : list) {
            CompoundTag entryTag = (CompoundTag) element;
            String name = entryTag.getString("name");
            BlockPos pos = new BlockPos(
                    entryTag.getInt("x"),
                    entryTag.getInt("y"),
                    entryTag.getInt("z")
            );
            data.servers.put(name, pos);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Map.Entry<String, BlockPos> entry : servers.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("name", entry.getKey());
            entryTag.putInt("x", entry.getValue().getX());
            entryTag.putInt("y", entry.getValue().getY());
            entryTag.putInt("z", entry.getValue().getZ());
            list.add(entryTag);
        }

        tag.put("servers", list);
        return tag;
    }

    public void addServer(String name, BlockPos pos) {
        if (!servers.containsKey(name)) {
            servers.put(name, pos);
            setDirty();
        }
    }

    public boolean removeServer(BlockPos blockPos) {
        String keyToRemove = null;

        for (Map.Entry<String, BlockPos> entry : servers.entrySet()) {
            if (entry.getValue().equals(blockPos)) {
                keyToRemove = entry.getKey();
                break;
            }
        }

        if (keyToRemove != null) {
            servers.remove(keyToRemove);
            setDirty();
            return true;
        }

        return false;
    }


    public Map<String, BlockPos> getServers() {
        return servers;
    }

}
