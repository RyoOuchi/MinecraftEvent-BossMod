package com.example.examplemod.DoNotTouch.Events;

import com.example.examplemod.DoNotTouch.ImportantConstants;
import com.example.examplemod.DoNotTouch.Networking.NetworkUtils;
import com.example.examplemod.DoNotTouch.ServerData.BossIDSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BossDamageHandler {

    @SubscribeEvent
    public static void onBossDamaged(LivingHurtEvent event) {
        final Entity entity = event.getEntity();
        final Level level = entity.level;
        if (level.isClientSide) return;
        if (!(entity.getType().equals(ImportantConstants.BOSS_ENTITY_TYPE))) return;
        final LivingEntity boss = (LivingEntity) entity;
        if (boss.getPersistentData().getBoolean("HasBeenHitOnce")) return;

        boss.getPersistentData().putBoolean("HasBeenHitOnce", true);
        System.out.println("[BossDamageHandler] FIRST HIT!");
        NetworkUtils.informBackendStartedBossFight(boss.getId(), event.getEntity().level);
        BossIDSavedData bossIDSavedData = BossIDSavedData.get((ServerLevel) level);
        bossIDSavedData.addBossId(boss.getId());
    }
}