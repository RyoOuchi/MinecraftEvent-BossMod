package com.example.examplemod.ServerData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class DnsServerSavedData extends SavedData {
    private final List<BlockPos> dnsServers = new ArrayList<>();

    public DnsServerSavedData() {}

    public static DnsServerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DnsServerSavedData::load, DnsServerSavedData::new, "dns_server_data");
    }

    public static DnsServerSavedData load(CompoundTag tag) {
        DnsServerSavedData data = new DnsServerSavedData();
        ListTag list = tag.getList("dnsServers", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag posTag = (CompoundTag) element;
            BlockPos pos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
            data.dnsServers.add(pos);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag list = new ListTag();
        for (BlockPos pos : dnsServers) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            list.add(posTag);
        }
        pCompoundTag.put("dnsServers", list);
        return pCompoundTag;
    }

    public void addDnsServer(BlockPos pos) {
        if (!dnsServers.contains(pos)) {
            dnsServers.add(pos);
            setDirty();
        }
    }

    public void removeDnsServer(BlockPos pos) {
        if (dnsServers.remove(pos)) {
            setDirty();
        }
    }

    public List<BlockPos> getDnsServers() {
        return dnsServers;
    }

    public boolean hasDnsServers() {
        return !dnsServers.isEmpty();
    }
}
