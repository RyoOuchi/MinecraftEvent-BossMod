package com.example.examplemod.Packet;

import com.example.examplemod.Blocks.BrowserBlock.BrowserBlockEntity;
import com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlockEntity;
import com.example.examplemod.Screens.BrowserScreen.BrowserSearchScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestUrlPacket {
    private final byte[] urlByte;
    private final BlockPos pos;

    public RequestUrlPacket(byte[] urlByte, BlockPos pos) {
        this.urlByte = urlByte;
        this.pos = pos;
    }

    public static void encode(RequestUrlPacket msg, FriendlyByteBuf buf) {
        buf.writeByteArray(msg.urlByte);
        buf.writeBlockPos(msg.pos);
    }

    public static RequestUrlPacket decode(FriendlyByteBuf buf) {
        byte[] urlBytes = buf.readByteArray();
        BlockPos pos = buf.readBlockPos();
        return new RequestUrlPacket(urlBytes, pos);
    }


    public static void handle(RequestUrlPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.sendMessage(new TextComponent("Sent to server side for processing: " + msg.urlByte), player.getUUID());

            AABB aabb = new AABB(msg.pos).inflate(5);

            double closestDistSq = Double.MAX_VALUE;
            WifiRouterBlockEntity closestRouter = null;

            for (BlockPos blockPos : BlockPos.betweenClosed(
                    (int) aabb.minX, (int) aabb.minY, (int) aabb.minZ,
                    (int) aabb.maxX, (int) aabb.maxY, (int) aabb.maxZ
            )) {
                if (player.level.getBlockEntity(blockPos) instanceof WifiRouterBlockEntity router) {
                    double distSq = blockPos.distSqr(msg.pos);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestRouter = router;
                    }
                }
            }

            if (closestRouter != null) {
                player.sendMessage(new TextComponent("Closest router found at: " + closestRouter.getBlockPos()), player.getUUID());

                // 👉 now you can call methods on closestRouter, e.g.:
                // closestRouter.handleRequest(msg.url, player);
                closestRouter.performDNSRequest(msg.urlByte, null, msg.pos);

            } else {
                player.sendMessage(new TextComponent("No WifiRouter found nearby!"), player.getUUID());
            }

            final BlockEntity be = player.level.getBlockEntity(msg.pos);
            if (!(be instanceof BrowserBlockEntity browserBlockEntity)) return;

            browserBlockEntity.setUrlData(msg.urlByte);

        });
        ctx.get().setPacketHandled(true);
    }

}
