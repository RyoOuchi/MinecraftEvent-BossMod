package com.example.examplemod.Events;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.ServerData.ServerSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class OnMinecraftInstanceLaunched extends Event {

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        System.out.println("World instance launched!");
        final var levelAccessor = event.getWorld();

        if (levelAccessor instanceof Level level && !level.isClientSide()) {
            System.out.println("Server world loaded: " + level.dimension().location());

            final ServerSavedData data = ServerSavedData.get((ServerLevel) level);
            List<BlockPos> toRemove = getBlockPos(level, data);

            // Now remove after iteration
            toRemove.forEach(data::removeServer);

            if (!toRemove.isEmpty()) {
                System.out.println("🧹 Cleaned up " + toRemove.size() + " invalid servers from saved data.");
            }
        }
    }

    private static List<BlockPos> getBlockPos(Level level, ServerSavedData data) {
        final var servers = data.getServers();

        // Collect invalid servers first (avoid ConcurrentModificationException)
        List<BlockPos> toRemove = new ArrayList<>();

        servers.forEach((serverName, serverBlockPos) -> {
            final var block = level.getBlockState(serverBlockPos).getBlock();
            if (block.equals(ExampleMod.SERVER_BLOCK)) {
                System.out.println("✅ Server [" + serverName + "] is valid at " + serverBlockPos);
            } else {
                System.out.println("❌ Server [" + serverName + "] is invalid at " + serverBlockPos + ". Marking for removal.");
                toRemove.add(serverBlockPos);
            }
        });
        return toRemove;
    }

}
