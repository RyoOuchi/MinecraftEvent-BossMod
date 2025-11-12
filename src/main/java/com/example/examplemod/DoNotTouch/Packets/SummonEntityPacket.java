package com.example.examplemod.DoNotTouch.Packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SummonEntityPacket {
    public SummonEntityPacket() {}
    public static void encode(SummonEntityPacket pkt, FriendlyByteBuf buf) {}
    public static SummonEntityPacket decode(FriendlyByteBuf buf) {
        return new SummonEntityPacket();
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                Level level = player.level;
                var pig = EntityType.PIG.create(level);
                if (pig != null) {
                    pig.setPos(player.getX(), player.getY(), player.getZ());
                    level.addFreshEntity(pig);
                    System.out.println("🐷 [Server] Pig spawned for " + player.getName().getString());
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
