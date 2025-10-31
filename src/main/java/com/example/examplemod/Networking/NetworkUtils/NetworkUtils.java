package com.example.examplemod.Networking.NetworkUtils;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.Networking.Data.Body;
import com.example.examplemod.Networking.Data.Header;
import com.example.examplemod.Networking.Data.Message;
import com.example.examplemod.Networking.Enums.Queries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NetworkUtils {
    private NetworkUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static List<BlockPos> findCablePathBetweenRouters(Level level, BlockPos startRouter, BlockPos endRouter) {
        List<BlockPos> cablePath = new ArrayList<>();
        if (level == null) return cablePath;

        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, BlockPos> previous = new HashMap<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(startRouter);
        visited.add(startRouter);

        boolean found = false;

        // BFS through cables
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            if (current.equals(endRouter)) {
                found = true;
                break;
            }

            for (BlockPos neighbor : getNeighbors(current)) {
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                BlockState state = level.getBlockState(neighbor);
                Block block = state.getBlock();
                BlockEntity be = level.getBlockEntity(neighbor);

                if (isCableBlock(block) || neighbor.equals(endRouter)) {
                    queue.add(neighbor);
                    previous.put(neighbor, current);
                }
            }
        }

        // Reconstruct the path of cables
        if (found) {
            BlockPos current = endRouter;
            while (previous.containsKey(current)) {
                BlockPos prev = previous.get(current);
                Block block = level.getBlockState(current).getBlock();

                if (isCableBlock(block)) {
                    cablePath.add(current);
                }
                current = prev;
            }
            Collections.reverse(cablePath);
        }

        System.out.println("🧵 Found " + cablePath.size() + " cable blocks between routers.");
        return cablePath;
    }

    public static boolean isCableBlock(Block block) {
        return block.equals(ExampleMod.CABLE_BLOCK);
    }

    public static BlockPos[] getNeighbors(BlockPos pos) {
        return new BlockPos[] {
                pos.above(),
                pos.below(),
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west()
        };
    }

    @Nullable
    public static BlockPos getNextRouterBlockPos(final List<BlockPos> path, final BlockPos currentPos) {
        if (path == null || path.isEmpty()) return null;

        // Find the index of the current router in the path
        final int currentIndex = path.indexOf(currentPos);

        if (currentIndex == -1) {
            System.out.println("⚠️ Current router position not found in path: " + currentPos);
            return null;
        }

        // Make sure there's another router after this one
        if (currentIndex >= path.size() - 1) {
            System.out.println("🏁 Reached the end of the router path.");
            return null;
        }

        // The next router position is one step ahead in the path
        final BlockPos nextRouterPos = path.get(currentIndex + 1);
        System.out.println("➡️ Next router block position in path: " + nextRouterPos);

        return nextRouterPos;
    }

    public static List<BlockPos> getCablePathToNextRouter(final Level level, final List<BlockPos> path, final BlockPos currentPos) {
        final BlockPos nextRouterPos = getNextRouterBlockPos(path, currentPos);
        return findCablePathBetweenRouters(level, currentPos, nextRouterPos);
    }

    // ==============================
    // === Packet Construction ===
    // ==============================

    /**
     * Creates a TCP packet message from header fields and data body
     */
    public static byte[] createTcpPacket(final int seqNumber, final int ackNumber, final boolean isLast, final byte[] data) {
        return createTcpPacket(seqNumber, ackNumber, -1, isLast, data);
    }

    /**
     * Creates a TCP packet message with response number
     */
    public static byte[] createTcpPacket(final int seqNumber, final int ackNumber, final int responseNumber, final boolean isLast, final byte[] data) {
        final Map<String, String> headerMap = new HashMap<>();
        headerMap.put("SEQ", Integer.toString(seqNumber));
        headerMap.put("ACK", Integer.toString(ackNumber));
        if (responseNumber != -1) {
            headerMap.put("RESPONSE", Integer.toString(responseNumber));
        }
        headerMap.put("END", isLast ? Integer.toString(1) : Integer.toString(-1));

        final Header header = new Header(headerMap);
        final Body body = new Body(data);
        final Message message = new Message(header, body);

        return message.constructMessage().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Creates an ACK packet
     */
    public static byte[] createAckPacket(final int ackNumber) {
        final Header ackHeader = new Header(Map.of("ACK", Integer.toString(ackNumber)));
        return ackHeader.getHeader().getBytes(StandardCharsets.UTF_8);
    }

    // ==============================
    // === Data Reassembly ===
    // ==============================

    /**
     * Reconstructs a string from ordered data chunks
     */
    public static String reconstructData(final Map<Integer, Body> dataChunks) {
        return dataChunks.values().stream()
                .map(Body::extractBody)
                .reduce("", String::concat);
    }

    /**
     * Checks if data chunks form a contiguous sequence without gaps
     */
    public static boolean isDataContiguous(final Map<Integer, Body> dataChunks) {
        if (dataChunks == null || dataChunks.isEmpty()) {
            return false;
        }

        final List<Integer> sortedSeqs = new ArrayList<>(dataChunks.keySet());
        Collections.sort(sortedSeqs);

        for (int i = 0; i < sortedSeqs.size() - 1; i++) {
            final int currentSeq = sortedSeqs.get(i);
            final Body currentBody = dataChunks.get(currentSeq);
            final int nextSeq = sortedSeqs.get(i + 1);

            final int currentEnd = currentSeq + currentBody.extractBody().getBytes(StandardCharsets.UTF_8).length;

            if (nextSeq != currentEnd) {
                return false;
            }
        }

        return true;
    }

    // ==============================
    // === Packet Processing ===
    // ==============================

    /**
     * Extracts header from packet data
     */
    public static Header extractHeader(final byte[] packetData) {
        return new Header(packetData);
    }

    /**
     * Extracts message from packet data
     */
    public static Message extractMessage(final byte[] packetData) {
        return new Message(packetData);
    }

    /**
     * Creates data chunks from byte array with specified chunk size
     */
    public static List<byte[]> createDataChunks(final byte[] data, final int chunkSize) {
        final List<byte[]> chunks = new ArrayList<>();
        for (int i = 0; i < data.length; i += chunkSize) {
            final int end = Math.min(data.length, i + chunkSize);
            final byte[] chunk = new byte[end - i];
            System.arraycopy(data, i, chunk, 0, end - i);
            chunks.add(chunk);
        }
        return chunks;
    }

}
