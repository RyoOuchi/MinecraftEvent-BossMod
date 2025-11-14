package com.example.examplemod.DoNotTouch.Packets;

import com.example.examplemod.DoNotTouch.ImportantConstants;
import com.example.examplemod.DoNotTouch.ServerData.TeamSavedData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;

public class SummonEntityPacket {
    private final String message;

    public SummonEntityPacket(String message) {
        this.message = message;
    }

    public static void encode(SummonEntityPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.message);
    }

    public static SummonEntityPacket decode(FriendlyByteBuf buf) {
        return new SummonEntityPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                final ServerLevel serverLevel = player.getLevel();
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> data = gson.fromJson(message, type);

                String teamID = data.get("teamName");
                String savedTeamID = TeamSavedData.get(serverLevel).getTeamId();

                if (!teamID.equals(savedTeamID)) {
                    System.out.println("[SummonEntityPacket] Team ID mismatch for " + player.getName().getString() + ". Expected: " + savedTeamID + ", Received: " + teamID);
                    return;
                }

                final Level level = player.level;
                var entity = ImportantConstants.BOSS_ENTITY_TYPE.create(level);
                if (entity != null) {
                    entity.setPos(player.getX(), player.getY(), player.getZ());
                    level.addFreshEntity(entity);
                    System.out.println("[SummonEntityPacket] Boss spawned for " + player.getName().getString() + ", With ID of: " + entity.getId());
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
