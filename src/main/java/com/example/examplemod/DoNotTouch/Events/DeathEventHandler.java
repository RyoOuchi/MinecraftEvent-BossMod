package com.example.examplemod.DoNotTouch.Events;

import com.example.examplemod.DoNotTouch.Networking.NetworkUtils;
import com.example.examplemod.DoNotTouch.ServerData.TeamSavedData;
import com.example.examplemod.DoNotTouch.apolloboss.ApolloBoss;
import jdk.jfr.Event;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DeathEventHandler {
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getSource().getEntity() instanceof ApolloBoss)) return;
        if (!(event.getEntity().level instanceof ServerLevel serverLevel)) return;
        final String teamID = TeamSavedData.get(serverLevel).getTeamId();
        NetworkUtils.informDiscordOfKilledByBoss(teamID);
    }
}
