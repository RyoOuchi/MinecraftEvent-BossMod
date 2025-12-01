package com.example.examplemod.DoNotTouch.Packets;

import com.example.examplemod.DoNotTouch.ImportantConstants;
import com.example.examplemod.DoNotTouch.ServerData.TeamSavedData;
import com.example.examplemod.DoNotTouch.TickScheduler.Scheduler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;

public class SummonEntityPacket {
    private final String message;

    public SummonEntityPacket(String message) {
        this.message = message;
    }

    public static void encode(SummonEntityPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.message);
    }

    public static SummonEntityPacket decode(FriendlyByteBuf buf) {
        return new SummonEntityPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                final ServerLevel serverLevel = player.getLevel();
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> data = gson.fromJson(message, type);

                String teamID = data.get("teamName");
                String savedTeamID = TeamSavedData.get(serverLevel).getTeamId();

                if (!teamID.equals(savedTeamID)) {
                    System.out.println("[SummonEntityPacket] Team ID mismatch for " + player.getName().getString() + ". Expected: " + savedTeamID + ", Received: " + teamID);
                    return;
                }

                final Level level = player.level;
                var entity = ImportantConstants.BOSS_ENTITY_TYPE.create(level);
                if (entity != null) {

                    Vec3 eyePos = player.getEyePosition();
                    Vec3 look = player.getLookAngle();
                    double distance = 10.0;

                    Vec3 spawnPos = eyePos.add(look.scale(distance));
                    double x = spawnPos.x;
                    double z = spawnPos.z;
                    int startY = (int) (player.getY() + 30);

                    int surfaceY = findSurfaceY(level, x, z, startY);
                    clearAboveSurface(level, x, z, surfaceY, startY);

                    entity.setPos(x, startY + 1, z);
                    level.addFreshEntity(entity);

                    for (int t = 0; t < 20 * 2; t += 10) {
                        Scheduler.schedule(t, () -> {
                            spawnRedParticleRing(serverLevel, x, surfaceY + 1.0, z, 3.0);
                            spawnRedParticleRing(serverLevel, x, surfaceY + 1.0, z, 5.0);
                            spawnRedParticleRing(serverLevel, x, surfaceY + 1.0, z, 7.0);

                            for (int i = 1; i < 40; i += 2) {
                                spawnRedParticleRing(serverLevel, x, surfaceY + 1.0 + i, z, 3.0);
                            }
                        });
                    }

                }
            }
        });
        context.get().setPacketHandled(true);
    }

    private static int findSurfaceY(Level level, double x, double z, int startY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);

        while (pos.getY() > level.getMinBuildHeight()) {

            BlockState current = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());

            boolean isSurface = above.isAir() && isSurfaceBlock(current);

            if (isSurface) {
                return pos.getY();
            }

            pos.move(0, -1, 0);
        }

        return level.getMinBuildHeight();
    }

    private static void clearAboveSurface(Level level, double x, double z, int surfaceY, int startY) {

        int bx = Mth.floor(x);
        int bz = Mth.floor(z);

        for (int y = surfaceY + 1; y <= startY; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos bp = new BlockPos(bx + dx, y, bz + dz);
                    BlockState blockState = level.getBlockState(bp);
                    if (isSurfaceBlock(blockState)) continue;
                    level.removeBlock(bp, false);
                }
            }
        }
    }

    private static boolean isSurfaceBlock(BlockState state) {
        Block block = state.getBlock();

        return block == Blocks.GRASS_BLOCK ||
                block == Blocks.DIRT ||
                block == Blocks.COARSE_DIRT ||
                block == Blocks.PODZOL ||
                block == Blocks.MYCELIUM ||
                block == Blocks.SAND ||
                block == Blocks.RED_SAND ||
                block == Blocks.GRAVEL ||
                block == Blocks.SNOW_BLOCK ||
                block == Blocks.STONE ||
                block == Blocks.DEEPSLATE ||
                block == Blocks.ANDESITE ||
                block == Blocks.DIORITE ||
                block == Blocks.GRANITE;
    }

    private static void spawnRedParticleRing(ServerLevel level, double cx, double y, double cz, double radius) {

        for (int angle = 0; angle < 360; angle += 4) {
            double rad = Math.toRadians(angle);

            double x = cx + Math.cos(rad) * radius;
            double z = cz + Math.sin(rad) * radius;

            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F),
                    x, y, z,
                    3,
                    0, 0, 0,
                    0.0D
            );
        }
    }

}
