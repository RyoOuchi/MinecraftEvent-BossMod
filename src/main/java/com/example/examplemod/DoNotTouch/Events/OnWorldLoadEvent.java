package com.example.examplemod.DoNotTouch.Events;

import com.example.examplemod.DoNotTouch.Networking.WebSocketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class OnWorldLoadEvent {
    private static final WebSocketManager WS_MANAGER = new WebSocketManager();
    private static final String WEBSOCKET_URL = "wss://7aadca023631.ngrok-free.app";

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerLevel level) {
            if (level.dimension().location().toString().equals("minecraft:overworld")) {
                System.out.println("🌍 World loaded — connecting to WebSocket");
                WS_MANAGER.connect(WEBSOCKET_URL);
            }
        }

    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof ServerLevel level) {
            if (level.dimension().location().toString().equals("minecraft:overworld")) {
                System.out.println("🧹 World unloading — closing WebSocket");
                WS_MANAGER.disconnect();
            }
        }
    }
}