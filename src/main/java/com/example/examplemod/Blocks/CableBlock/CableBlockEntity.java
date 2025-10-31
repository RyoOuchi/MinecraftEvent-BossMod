package com.example.examplemod.Blocks.CableBlock;

import com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlockEntity;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Enums.Senders;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CableBlockEntity extends BlockEntity {

    /** A single queue where each enqueued item carries its own sender metadata. */
    private final Queue<PacketFrame> packetQueue = new LinkedList<>();
    /** The frame (packet + sender) currently being transmitted along this cable block. */
    private PacketFrame currentFrame;

    /** Lightweight wrapper to keep sender metadata per packet. */
    private static final class PacketFrame {
        final DataPacket packet;
        final Senders sender;
        PacketFrame(DataPacket packet, Senders sender) {
            this.packet = packet;
            this.sender  = sender;
        }
    }

    public CableBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ExampleMod.CABLE_BLOCK_ENTITY, pWorldPosition, pBlockState);
    }

    // ==============================
    // === PACKET HANDLING LOGIC ===
    // ==============================

    public void enqueuePacket(DataPacket packet, Senders sender) {
        if (packet == null || sender == null) return;
        packetQueue.offer(new PacketFrame(packet, sender));
        System.out.println("📥 Queued packet at " + worldPosition + " from " + sender + ": " + safeDataString(packet));
        System.out.println("📥 Queue size now: " + packetQueue.size());
        packetQueue.forEach(f -> System.out.println("    - [" + f.sender + "] " + safeDataString(f.packet)));
    }

    private void processNextPacket() {
        if (currentFrame == null && !packetQueue.isEmpty()) {
            packetQueue.forEach(f -> System.out.println("    - Pending [" + f.sender + "] " + safeDataString(f.packet)));
            currentFrame = packetQueue.poll();
            System.out.println("📦 Processing next at " + worldPosition);
            System.out.println("📦 Current [" + currentFrame.sender + "] " + safeDataString(currentFrame.packet));
        }
    }

    public void tickServer(final Level level, final BlockPos pos) {
        if (level.isClientSide) return;

        // 1️⃣ Fetch next frame if idle
        processNextPacket();
        if (currentFrame == null) return;

        final DataPacket dataPacket = currentFrame.packet;
        System.out.println("🚚 Transmitting at " + pos + " | [" + currentFrame.sender + "] " + safeDataString(dataPacket));

        final List<BlockPos> cablePath = dataPacket.getCablePath();
        if (cablePath == null || cablePath.isEmpty()) { clearCurrentPacket(); return; }

        final int currentIndex = cablePath.indexOf(pos);
        if (currentIndex == -1) { clearCurrentPacket(); return; }

        final BlockPos nextPos = (currentIndex < cablePath.size() - 1) ? cablePath.get(currentIndex + 1) : null;

        if (nextPos == null) {
            // Reached end of this cable path -> handoff to adjacent router
            System.out.println("✅ Packet reached end of path at " + pos);
            sendPacketToRouterEntity(pos, dataPacket);
            clearCurrentPacket();
            return;
        }

        // Forward to next cable if available
        final CableBlockEntity nextCable = getCableEntityAtPosition(nextPos);
        if (nextCable != null) {
            // Preserve provenance; from now on the sender is a cable hop
            nextCable.enqueuePacket(dataPacket, Senders.CABLE);
            System.out.println("➡️ Handoff " + pos + " → " + nextPos + " | [" + currentFrame.sender + "]");
            clearCurrentPacket();
        }
    }

    // ===========================
    // === HELPER FUNCTIONS ===
    // ===========================

    private void clearCurrentPacket() {
        this.currentFrame = null;
    }

    @Nullable
    private CableBlockEntity getCableEntityAtPosition(final BlockPos pos) {
        if (pos == null || level == null) return null;
        final BlockEntity be = level.getBlockEntity(pos);
        return (be instanceof CableBlockEntity cable) ? cable : null;
    }

    private void sendPacketToRouterEntity(final BlockPos currentPos, final DataPacket packet) {
        final List<BlockPos> routerPath = packet.getRouterPath();
        if (routerPath == null || routerPath.isEmpty()) return;

        for (BlockPos routerPos : routerPath) {
            if (routerPos.distManhattan(currentPos) <= 1) {
                final BlockEntity be = level.getBlockEntity(routerPos);
                if (be instanceof WifiRouterBlockEntity router) {
                    router.receiveTransmittedPacket(packet, this);
                    System.out.println("📡 Cable " + currentPos + " → Router " + routerPos + " | payload: " + safeDataString(packet));
                    return;
                }
            }
        }
    }

    private static String safeDataString(DataPacket p) {
        try {
            return new String(p.getData());
        } catch (Exception e) {
            return "<binary " + (p.getData() == null ? 0 : p.getData().length) + " bytes>";
        }
    }

    // ===========================
    // === DEBUG HELPERS ===
    // ===========================

    public boolean hasDataPacket() {
        return currentFrame != null;
    }

    /** Deprecated: sender is tracked per-packet now. Kept only if other code calls it. */
    @Deprecated
    public void setSender(Senders sender) { /* no-op */ }

    /** Deprecated: sender is tracked per-packet now. Kept only if other code calls it. */
    @Deprecated
    public void setSenderToNull() { /* no-op */ }

    public int getQueueSize() {
        return packetQueue.size();
    }
}