package com.example.examplemod.DoNotTouch.Events;

import com.example.examplemod.DoNotTouch.ImportantConstants;
import com.example.examplemod.DoNotTouch.Networking.NetworkUtils;
import com.example.examplemod.DoNotTouch.ServerData.BossIDSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

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
        BossIDSavedData bossIDSavedData = BossIDSavedData.get((ServerLevel) level);
        bossIDSavedData.getBossIds().forEach((integer) -> {
            if (integer.equals(boss.getId())) {
                bossIDSavedData.removeBossId(integer);
                NetworkUtils.informBackendDefeatedBoss(boss.getId(), level);
                System.out.println("[BossDeathHandler] Informed backend of boss defeat and removed boss ID from saved data.");
            }
        });
    }
}