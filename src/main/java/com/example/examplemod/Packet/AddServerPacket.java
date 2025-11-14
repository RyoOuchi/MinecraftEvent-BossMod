package com.example.examplemod.Packet;

import com.example.examplemod.ServerData.ServerSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AddServerPacket {
    private final String domain;
    private final BlockPos pos;

    public AddServerPacket(String domain, BlockPos pos) {
        this.domain = domain;
        this.pos = pos;
    }

    public static void encode(AddServerPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.domain);
        buf.writeBlockPos(msg.pos);
    }

    public static AddServerPacket decode(FriendlyByteBuf buf) {
        return new AddServerPacket(buf.readUtf(), buf.readBlockPos());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            final ServerLevel serverLevel = player.getLevel();
            final ServerSavedData data = ServerSavedData.get(serverLevel);

            data.addServer(domain, pos);

            System.out.println("✅ Added server [" + domain + "] at " + pos);
        });
        context.get().setPacketHandled(true);
    }
}
