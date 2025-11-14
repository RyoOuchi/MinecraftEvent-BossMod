package com.example.examplemod.Packet;

import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TerminalCommandPacket {
    private final BlockPos pos;
    private final String command;

    public TerminalCommandPacket(BlockPos pos, String command) {
        this.pos = pos;
        this.command = command;
    }

    public static void encode(TerminalCommandPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.command);
    }

    public static TerminalCommandPacket decode(FriendlyByteBuf buf) {
        return new TerminalCommandPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(TerminalCommandPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            var level = player.level;
            if (!(level.getBlockEntity(packet.pos) instanceof ServerBlockEntity serverEntity)) return;

            String result = serverEntity.executeCommand(packet.command);

            serverEntity.sendTerminalOutputToClient(player, result);
        });
        ctx.get().setPacketHandled(true);
    }
}
