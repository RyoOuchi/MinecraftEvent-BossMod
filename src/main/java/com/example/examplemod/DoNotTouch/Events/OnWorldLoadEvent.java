package com.example.examplemod.DoNotTouch.Events;

import com.example.examplemod.DoNotTouch.Networking.WebSocketManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.example.examplemod.DoNotTouch.Networking.WebSocketManager.WEBSOCKET_URL;


@Mod.EventBusSubscriber
public class OnWorldLoadEvent {
    private static final WebSocketManager WS_MANAGER = new WebSocketManager();

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerLevel level) {
            if (level.dimension().location().toString().equals("minecraft:overworld")) {
                System.out.println("[OnWorldLoadEvent] World loaded — connecting to WebSocket");
                WS_MANAGER.connect(WEBSOCKET_URL);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof ServerLevel level) {
            if (level.dimension().location().toString().equals("minecraft:overworld")) {
                System.out.println("[OnWorldLoadEvent] World unloading — closing WebSocket");
                WS_MANAGER.disconnect();
            }
        }
    }
}