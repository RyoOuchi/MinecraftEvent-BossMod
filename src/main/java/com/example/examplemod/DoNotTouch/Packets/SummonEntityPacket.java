package com.example.examplemod.DoNotTouch.Packets;

import com.example.examplemod.DoNotTouch.ImportantConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
