package com.example.examplemod.DoNotTouch.Events;

import com.example.examplemod.DoNotTouch.Packets.OpenGuiPacket;
import com.example.examplemod.DoNotTouch.ServerData.TeamSavedData;
import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber
public class PlayerLogsInEvent {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final ServerLevel serverLevel = serverPlayer.getLevel();
        final TeamSavedData teamSavedData = TeamSavedData.get(serverLevel);
        final String teamId = teamSavedData.getTeamId();

        if (teamId.isEmpty()) {
            ExampleMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new OpenGuiPacket());
            return;
        }
        System.out.println("[PlayerLogsInEvent] Already assigned to team: " + teamId);
        serverPlayer.sendMessage(new TextComponent("チーム：" + teamId), serverPlayer.getUUID());
    }
}
