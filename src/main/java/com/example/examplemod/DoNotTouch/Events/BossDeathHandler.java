package com.example.examplemod.DoNotTouch.Events;

import com.example.examplemod.DoNotTouch.ImportantConstants;
import com.example.examplemod.DoNotTouch.Networking.NetworkUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BossDeathHandler {

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        final Entity entity = event.getEntity();
        final Level level = entity.level;
        if (level.isClientSide) return;
        if (!entity.getType().equals(ImportantConstants.BOSS_ENTITY_TYPE)) return;
        System.out.println("[BossDeathHandler] Boss has died!");
        final LivingEntity boss = (LivingEntity) entity;
        NetworkUtils.informBackendDefeatedBoss(boss.getId(), level);
    }
}