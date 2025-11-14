package com.example.examplemod.DoNotTouch.ServerData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class TeamSavedData extends SavedData {
    private static final String DATA_NAME = "team_data";
    private String teamId = "";

    public TeamSavedData() {}

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
        this.setDirty();
    }

    public static TeamSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                TeamSavedData::load,
                TeamSavedData::new,
                DATA_NAME
        );
    }

    public static TeamSavedData load(CompoundTag tag) {
        TeamSavedData data = new TeamSavedData();
        if (tag.contains("teamId")) {
            data.teamId = tag.getString("teamId");
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("teamId", teamId);
        return tag;
    }
}