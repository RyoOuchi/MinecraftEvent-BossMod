package com.example.examplemod.DoNotTouch.Packets;

import com.example.examplemod.DoNotTouch.ServerData.TeamSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfirmTeamPacket {
    private final String teamId;

    public ConfirmTeamPacket(final String teamId) {
        this.teamId = teamId;
    }

    public static void encode(ConfirmTeamPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.teamId);
    }
    public static ConfirmTeamPacket decode(FriendlyByteBuf buf) {
        return new ConfirmTeamPacket(buf.readUtf());
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            String teamSendId = teamId.startsWith("Team ") ? teamId.substring(5) : "None";
            final ServerPlayer player = context.get().getSender();
            if (player == null) return;
            // save teamid to saved data. Team id will now persist across logins.
            TeamSavedData teamSavedData = TeamSavedData.get(player.getLevel());
            teamSavedData.setTeamId(teamSendId);
        });
        context.get().setPacketHandled(true);
    }
}
