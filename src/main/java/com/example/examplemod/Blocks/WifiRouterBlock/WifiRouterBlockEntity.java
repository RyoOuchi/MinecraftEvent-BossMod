package com.example.examplemod.Blocks.WifiRouterBlock;

import com.example.examplemod.Blocks.BrowserBlock.BrowserBlockEntity;
import com.example.examplemod.Blocks.CableBlock.CableBlockEntity;
import com.example.examplemod.Blocks.DNSServerBlock.DNSServerBlockEntity;
import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Enums.Senders;
import com.example.examplemod.Networking.Graph;
import com.example.examplemod.Networking.Enums.Queries;
import com.example.examplemod.ServerData.DnsServerSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WifiRouterBlockEntity extends BlockEntity {
    private final Graph routerMap = new Graph();
    private BlockPos dnsServerBlockPos;
    private final BlockPos currentPos;

    // variables that store the block entity state for transmitting the packet
    private boolean readyToTransmit = false;
    private DataPacket currentPacket = null;
    private CableBlockEntity currentTargetCable = null;

    private final Queue<TransmissionTask> transmissionQueue = new LinkedList<>();

    private static class TransmissionTask {
        final DataPacket packet;
        final CableBlockEntity target;
        TransmissionTask(DataPacket packet, CableBlockEntity target) {
            this.packet = packet;
            this.target = target;
        }
    }

    private static final int MAX_DEPTH = 3;

    public WifiRouterBlockEntity(final BlockPos pWorldPosition, final BlockState pBlockState) {
        super(ExampleMod.WIFI_ROUTER_ENTITY, pWorldPosition, pBlockState);
        this.currentPos = pWorldPosition;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.loadDNSServers();
    }

    public void loadDNSServers() {
        if (level instanceof ServerLevel serverLevel) {
            connectToRouterNetwork(currentPos);
            DnsServerSavedData data = DnsServerSavedData.get(serverLevel);
            List<BlockPos> servers = data.getDnsServers();

            if (servers.isEmpty()) {
                System.out.println("⚠️ No DNS servers registered in world.");
            } else {
                System.out.println("🌐 DNS servers in world:");
                for (BlockPos pos : servers) {
                    System.out.println("   - " + pos);
                }
                dnsServerBlockPos = getNearestDNSServerBlockPosition(servers, worldPosition);
                System.out.println("📡 Nearest DNS server: " + dnsServerBlockPos);
            }

            routerMap.visualizeNetwork(serverLevel, Blocks.AMETHYST_BLOCK);
        }
    }

    @Nullable
    private BlockPos getNearestDNSServerBlockPosition(final List<BlockPos> positions, final BlockPos routerPos) {
        if (positions == null || positions.isEmpty()) return null;

        // 1 Find DNS servers that already have a router underneath them
        final List<BlockPos> connectedDNSServers = positions.stream()
                .filter(pos -> hasRouterAtPosition(pos.below()))
                .toList();

        // 2 If no connected DNS servers exist → return the closest DNS block itself
        if (connectedDNSServers.isEmpty()) {
            System.out.println("⚠️ No DNS servers are connected to any router.");
            return positions.stream()
                    .min(Comparator.comparingDouble(position -> position.distSqr(routerPos)))
                    .orElse(null); // ✅ return the DNS block itself
        }

        // 3 Convert DNS server blocks → routers below for pathfinding
        final List<BlockPos> connectedDNSRouters = connectedDNSServers.stream()
                .map(BlockPos::below)
                .toList();

        // 4 Find the closest *router* under any DNS server
        final BlockPos nearestRouterUnderDNS = routerMap.findClosestTarget(currentPos, connectedDNSRouters);
        if (nearestRouterUnderDNS == null) {
            System.out.println("⚠️ No reachable DNS router found in network graph.");
            return positions.stream()
                    .min(Comparator.comparingDouble(position -> position.distSqr(routerPos)))
                    .orElse(null);
        }

        // 5 Find the DNS Server block above that router
        final BlockPos nearestDNSBlock = nearestRouterUnderDNS.above();
        System.out.println("📡 Closest DNS Server block: " + nearestDNSBlock);

        return nearestDNSBlock; // returns DNS Server Block Pos
    }

    public void performDNSRequest(final byte[] urlBytes, final List<BlockPos> oldPath, final BlockPos clientPos) {
        if (level == null || level.isClientSide()) return;
        this.loadDNSServers();
        if (!isConnectedToDNSServer()) {
            System.out.println("❌ Router is not connected to any DNS server. Cannot perform DNS request.");
            return;
        }

        final String url = new String(urlBytes, StandardCharsets.UTF_8);
        System.out.println("Received URL: " + url);
        System.out.println("Performing DNS request for: " + url);

        final BlockPos dnsRouterBlockPos = dnsServerBlockPos.below();
        if (!hasRouterAtPosition(dnsRouterBlockPos)) {
            System.out.println("❌ No router found at DNS server position: " + dnsRouterBlockPos);
            return;
        }

        BlockPos nearestRouter = routerMap.getRouters().stream()
                .min(Comparator.comparingDouble(pos -> pos.distSqr(dnsRouterBlockPos)))
                .orElse(null);
        if (nearestRouter == null) {
            System.out.println("⚠️ No routers found in network graph.");
            return;
        }

        System.out.println("📶 Closest router to DNS server: " + nearestRouter);

        List<BlockPos> path = routerMap.findShortestPath(currentPos, nearestRouter);
        if (path == null || path.isEmpty()) {
            System.out.println("❌ No path found from router to DNS server.");
            return;
        }

        visualizePathWithBlocks(path, Blocks.GLOWSTONE);
        visualizeNextRouterWithRedstone(path);

        final BlockPos nextRouterPos = getNextRouterBlockPos(path);
        if (nextRouterPos == null) {
            System.out.println("❌ No next router found in path.");
            return;
        }

        List<BlockPos> cableBlocks = findCablePathBetweenRouters(level, currentPos, nextRouterPos);
        System.out.println("🔌 Cable blocks between routers:");
        cableBlocks.forEach(pos -> System.out.println("   - " + pos));

        if (cableBlocks.isEmpty()) {
            System.out.println("❌ No cable blocks found to send the DNS request.");
            return;
        }

        // build packet
        final DataPacket dnsRequestPacket = new DataPacket(path, urlBytes, Queries.DNS, cableBlocks, currentPos, nearestRouter, clientPos);

        // merge with old path if present
        final DataPacket finalPacket;
        if (oldPath != null && !oldPath.isEmpty()) {
            oldPath.remove(oldPath.size() - 1);
            finalPacket = dnsRequestPacket.addNewRouterPathToOldPath(oldPath, path);
        } else finalPacket = dnsRequestPacket;

        final BlockPos firstCablePos = cableBlocks.get(0);
        final BlockEntity firstCableBE = level.getBlockEntity(firstCablePos);
        if (!(firstCableBE instanceof CableBlockEntity cableBlockEntity)) return;

        // ✅ enqueue instead of immediate send
        queueTransmission(finalPacket, cableBlockEntity);
        System.out.println("🚀 Queued DNS request for cable at: " + firstCablePos);
    }


    public void tickServer(final Level level) {
        if (level == null || level.isClientSide()) return;

        if (!readyToTransmit && !transmissionQueue.isEmpty()) {
            TransmissionTask nextTask = transmissionQueue.poll();
            if (nextTask != null && !nextTask.target.hasDataPacket()) {
                nextTask.target.enqueuePacket(nextTask.packet, Senders.ROUTER);

                readyToTransmit = true;
                currentPacket = nextTask.packet;
                currentTargetCable = nextTask.target;

                System.out.println("📡 Started transmission for packet: " + new String(currentPacket.getData()));
            } else if (nextTask != null) {
                // target busy, requeue
                transmissionQueue.add(nextTask);
                System.out.println("⏳ Target cable busy — requeued packet.");
            }
        }

        if (readyToTransmit && currentTargetCable != null) {
            if (!currentTargetCable.hasDataPacket()) {
                System.out.println("✅ Transmission complete for packet: " + new String(currentPacket.getData()));
                resetTransmissionState();
            }
        }
    }

    private void resetTransmissionState() {
        readyToTransmit = false;
        currentPacket = null;
        currentTargetCable = null;
    }

    // enqueue transmission task
    private void queueTransmission(DataPacket packet, CableBlockEntity cable) {
        if (packet == null || cable == null) return;
        transmissionQueue.add(new TransmissionTask(packet, cable));
        System.out.println("🕒 Queued transmission for packet: " + new String(packet.getData()));
    }


    public void receiveTransmittedPacket(final DataPacket packet, final CableBlockEntity cableBlockEntity) {
        if (packet == null) return;
        System.out.println("📥 Router received transmitted packet: " + new String(packet.getData()));


        final List<BlockPos> routerPath = packet.getRouterPath();
        if (routerPath == null || routerPath.isEmpty()) {
            System.out.println("⚠️ Received packet has no router path.");
            return;
        }

        final BlockPos nextRouterPos = getNextRouterBlockPos(routerPath);

        if (nextRouterPos == null) {
            // if the packet has an error code that means that the packet is a response packet
            // if not it's a request packet
            if (packet.getErrorCode() == null) {
                final Queries queryType = packet.getQueryType();
                switch (queryType) {
                    case DNS -> {
                        System.out.println("✅ Packet has reached its final router. Processing DNS response...");

                        if (!isDNSServerRouter()) {
                            System.out.println("❌ This router is not connected to any DNS server. Redirecting...");
                            performDNSRequest(packet.getData(), packet.getRouterPath(), packet.getClientBlockPos());
                            return;
                        }

                        final BlockPos assumedDNSServerBlockPos = currentPos.above();
                        final BlockEntity blockEntity = safeGetBlockEntity((ServerLevel) level, assumedDNSServerBlockPos);

                        if (blockEntity instanceof DNSServerBlockEntity dnsServerBlockEntity) {
                            dnsServerBlockEntity.receiveDataPacket(packet);
                        } else {
                            System.out.println("❌ DNS server block not found at: " + assumedDNSServerBlockPos);
                        }
                    }

                    case TCP_HANDSHAKE, TCP_ESTABLISHED, TCP_DISCONNECT -> {
                        System.out.println("✅ Packet has reached its final router. Processing request...");

                        if (!isServerRouter()) {
                            System.out.println("❌ This router is not connected to any server. Redirecting...");
                            performServerRequest(packet.getData(), packet.getSendToBlockPos(), packet.getRouterPath(), packet.getClientBlockPos(), Queries.TCP_HANDSHAKE);
                            return;
                        }

                        final BlockPos assumedServerBlockPos = currentPos.above();
                        final BlockEntity blockEntity = safeGetBlockEntity((ServerLevel) level, assumedServerBlockPos);

                        if (blockEntity instanceof ServerBlockEntity serverBlockEntity) {
                            serverBlockEntity.receiveDataPacket(packet);
                        } else {
                            System.out.println("❌ Server block not found at: " + assumedServerBlockPos);
                        }
                    }
                }
            } else {
                System.out.println("✅ Packet has reached its final router. Sending response to browser...");

                final BlockPos clientPos = packet.getClientBlockPos();
                if (clientPos == null) {
                    System.out.println("❌ No client position found in packet. Cannot send response.");
                    return;
                }
                final BlockEntity clientBE = safeGetBlockEntity((ServerLevel) level, clientPos);

                if (!(clientBE instanceof BrowserBlockEntity browserBlockEntity)) {
                    System.out.println("❌ No browser block entity found at client position: " + clientPos);
                    return;
                }

                final Queries queryType = packet.getQueryType();

                switch (queryType) {
                    case DNS -> browserBlockEntity.receiveDNSResponse(packet, this);
                    case TCP_HANDSHAKE -> browserBlockEntity.receiveHandshakeServerResponse(packet, this);
                    case TCP_ESTABLISHED -> browserBlockEntity.receiveEstablishedServerResponse(packet, this);
                    case TCP_DISCONNECT -> browserBlockEntity.receiveDisconnectServerResponse(packet, this);
                }

            }

            return;
        }

        System.out.println("➡️ Forwarding to next router at: " + nextRouterPos);
        final List<BlockPos> nextCablePath = findCablePathBetweenRouters(level, currentPos, nextRouterPos);

        if (nextCablePath.isEmpty()) {
            System.out.println("❌ No cable blocks found to forward packet.");
            return;
        }

        final DataPacket newDataPacket = packet.updateCablePath(nextCablePath);
        final BlockPos firstCablePos = nextCablePath.get(0);
        final BlockEntity firstCableBE = level.getBlockEntity(firstCablePos);
        if (!(firstCableBE instanceof CableBlockEntity nextCableBlockEntity)) return;

        queueTransmission(newDataPacket, nextCableBlockEntity);
    }

    public void performServerRequest(final byte[] filePathBytes, final BlockPos serverIPAddress, final List<BlockPos> oldRouterPath, final BlockPos clientPos, final Queries queryType) {
        if (level == null || level.isClientSide()) return;
        final BlockPos serverRouterBlockPos = serverIPAddress.below();

        final boolean serverRouterIsNotInMap = routerMap.getRouters().stream().noneMatch(blockPos -> blockPos.equals(serverRouterBlockPos));

        BlockPos sendToRouter;

        if (serverRouterIsNotInMap) {
            System.out.println("❌ Target server router is not in the network graph: " + serverRouterBlockPos);
            sendToRouter = routerMap.getRouters().stream()
                    .min(Comparator.comparingDouble(pos -> pos.distSqr(serverRouterBlockPos)))
                    .orElse(null);

        } else {
            sendToRouter = serverRouterBlockPos;
        }


        if (!hasRouterAtPosition(sendToRouter)) {
            System.out.println("❌ No router found at server IP position: " + sendToRouter);
            return;
        }

        final List<BlockPos> routerPath = routerMap.findShortestPath(currentPos, sendToRouter);

        final BlockPos nextRouterPos = getNextRouterBlockPos(routerPath);

        if (nextRouterPos == null) {
            System.out.println("❌ No path found from router to server.");
            return;
        }

        final List<BlockPos> cableBlocks = findCablePathBetweenRouters(level, currentPos, nextRouterPos);

        final DataPacket serverRequestPacket = new DataPacket(routerPath, filePathBytes, queryType, cableBlocks, currentPos, sendToRouter, clientPos);

        final DataPacket finalPacket;
        if (oldRouterPath != null && !oldRouterPath.isEmpty()) {
            oldRouterPath.remove(oldRouterPath.size() - 1);
            finalPacket = serverRequestPacket.addNewRouterPathToOldPath(oldRouterPath, routerPath);
        } else {
            finalPacket = serverRequestPacket;
        }

        final BlockPos firstCablePos = cableBlocks.get(0);
        final BlockEntity firstCableBE = level.getBlockEntity(firstCablePos);
        if (!(firstCableBE instanceof CableBlockEntity cableBlockEntity)) return;

        queueTransmission(finalPacket, cableBlockEntity);
        System.out.println("🚀 Queued server request for cable at: " + firstCablePos);
    }

    public void transmitPacket(final DataPacket dataPacket) {
        if (dataPacket == null) return;
        System.out.println("📤 Preparing to transmit packet: " + new String(dataPacket.getData()));

        final List<BlockPos> cablePath = dataPacket.getCablePath();
        if (cablePath == null || cablePath.isEmpty()) {
            System.out.println("❌ No cable path found in packet.");
            return;
        }

        final BlockPos firstCablePos = cablePath.get(0);
        final BlockEntity firstCableBE = level.getBlockEntity(firstCablePos);
        if (!(firstCableBE instanceof CableBlockEntity cableBlockEntity)) {
            System.out.println("❌ First cable block entity not found at: " + firstCablePos);
            return;
        }

        queueTransmission(dataPacket, cableBlockEntity);
        System.out.println("🚀 Queued packet for transmission to cable at: " + firstCablePos);
    }

    // force load chunk if not loaded
    private BlockEntity safeGetBlockEntity(ServerLevel serverLevel, BlockPos pos) {
        if (!serverLevel.hasChunkAt(pos)) {
            System.out.println("📦 Forcing chunk load for: " + pos);
            serverLevel.getChunkAt(pos); // Force chunk generation/loading
        }
        return serverLevel.getBlockEntity(pos);
    }


    /**
     * Finds all cable block positions that connect the current router to the next router.
     */
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


    @Nullable
    private BlockPos getNextRouterBlockPos(final List<BlockPos> path) {
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


    // for debugging purpose => visualize the next router block that the data will be sent to
    private void visualizeNextRouterWithRedstone(final List<BlockPos> path) {
        if (level == null || level.isClientSide()) return;
        final BlockPos nextRouterPos = getNextRouterBlockPos(path);
        if (nextRouterPos == null) return;
        final BlockPos aboveNextRouterPos = nextRouterPos.above(3);

        if (level.isEmptyBlock(aboveNextRouterPos)) {
            level.setBlock(aboveNextRouterPos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            System.out.println("🟥 Placed Redstone block at next router position: " + aboveNextRouterPos);
        }
    }


    // for debugging purposes -> visualize the path with glowstone blocks
    private void visualizePathWithBlocks(final List<BlockPos> path, final Block markerBlock) {
        if (level == null || level.isClientSide() || path == null) return;

        for (BlockPos pos : path) {
            BlockPos glowPos = pos.above(2);
            if (level.isEmptyBlock(glowPos)) {
                level.setBlock(glowPos, markerBlock.defaultBlockState(), 3);
            }
        }

        System.out.println("💡 Placed Glowstone markers for debug path (" + path.size() + " blocks).");
    }

    private boolean isConnectedToDNSServer() {
        return dnsServerBlockPos != null;
    }

    private boolean hasRouterAtPosition(final BlockPos pos) {
        final BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;
        return level.getBlockEntity(pos) instanceof WifiRouterBlockEntity;
    }

    /**
     * Connects this router to all reachable routers via cable paths (3 degrees max),
     * and ensures full connectivity between all routers that share any cable path.
     */
    public void connectToRouterNetwork(BlockPos startPos) {
        if (level == null || level.isClientSide()) return;

        System.out.println("🔍 Scanning router network starting from: " + startPos);
        routerMap.addRouter(startPos);

        Set<BlockPos> visitedRouters = new HashSet<>();
        Queue<RouterNode> queue = new LinkedList<>();
        queue.add(new RouterNode(startPos, 0));
        visitedRouters.add(startPos);

        // First BFS — discover routers and local connections
        while (!queue.isEmpty()) {
            RouterNode current = queue.poll();
            if (current.depth >= MAX_DEPTH) continue;

            Map<BlockPos, Integer> foundRouters = findConnectedRouters(level, current.pos);
            for (Map.Entry<BlockPos, Integer> entry : foundRouters.entrySet()) {
                BlockPos neighborRouter = entry.getKey();
                int cableLength = entry.getValue();

                // Add the routers and connect them
                routerMap.addRouter(neighborRouter);
                routerMap.addConnection(current.pos, neighborRouter, cableLength);
                System.out.println("🔗 Connected " + current.pos + " → " + neighborRouter + " (cables: " + cableLength + ")");

                // Continue BFS if new router
                if (!visitedRouters.contains(neighborRouter)) {
                    queue.add(new RouterNode(neighborRouter, current.depth + 1));
                    visitedRouters.add(neighborRouter);
                }
            }
        }

        // This ensures indirect routers (like {2,-60,14} → {7,-60,10}) are linked
        List<BlockPos> routers = new ArrayList<>(routerMap.getRouters());
        for (int i = 0; i < routers.size(); i++) {
            for (int j = i + 1; j < routers.size(); j++) {
                BlockPos a = routers.get(i);
                BlockPos b = routers.get(j);
                int distance = getCableDistance(level, a, b);
                if (distance > 0) {
                    routerMap.addConnection(a, b, distance);
                    System.out.println("🔁 Linked additional routers " + a + " ↔ " + b + " (cables: " + distance + ")");
                }
            }
        }

        System.out.println("✅ Network connection complete. Total routers: " + routerMap.getRouters().size());
    }

    /**
     * Finds the shortest cable-only distance between two routers.
     * Returns -1 if no cable connection exists between them.
     */
    private int getCableDistance(Level level, BlockPos start, BlockPos end) {
        if (start.equals(end) || level == null) return -1;

        Set<BlockPos> visited = new HashSet<>();
        Queue<PathNode> queue = new LinkedList<>();
        queue.add(new PathNode(start, 0));
        visited.add(start);

        while (!queue.isEmpty()) {
            PathNode node = queue.poll();
            BlockPos pos = node.pos;

            for (BlockPos neighbor : getNeighbors(pos)) {
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                BlockState state = level.getBlockState(neighbor);
                Block block = state.getBlock();
                BlockEntity be = level.getBlockEntity(neighbor);

                // Only traverse cables, but allow the end router
                if (isCableBlock(block)) {
                    queue.add(new PathNode(neighbor, node.distance + 1));
                } else if (neighbor.equals(end) && be instanceof WifiRouterBlockEntity) {
                    return node.distance + 1; // reached destination
                }
            }
        }
        return -1; // not connected
    }


    private Map<BlockPos, Integer> findConnectedRouters(Level level, BlockPos startRouter) {
        Map<BlockPos, Integer> foundRouters = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<PathNode> queue = new LinkedList<>();

        queue.add(new PathNode(startRouter, 0));
        visited.add(startRouter);

        while (!queue.isEmpty()) {
            PathNode node = queue.poll();

            for (BlockPos neighbor : getNeighbors(node.pos)) {
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                BlockState state = level.getBlockState(neighbor);
                Block block = state.getBlock();
                BlockEntity be = level.getBlockEntity(neighbor);

                if (isCableBlock(block)) {
                    queue.add(new PathNode(neighbor, node.distance + 1));
                } else if (isValidNode(be) && !neighbor.equals(startRouter)) {
                    foundRouters.put(neighbor, node.distance);
                }
            }
        }

        return foundRouters;
    }

    private boolean isValidNode(BlockEntity be) {
        return be instanceof WifiRouterBlockEntity || be instanceof DNSServerBlockEntity;
    }

    private static boolean isCableBlock(Block block) {
        return block.equals(ExampleMod.CABLE_BLOCK);
    }

    private static BlockPos[] getNeighbors(BlockPos pos) {
        return new BlockPos[] {
                pos.above(),
                pos.below(),
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west()
        };
    }

    public Graph getRouterMap() {
        return routerMap;
    }

    public boolean isDNSServerRouter() {
        final Block block = level.getBlockState(currentPos.above()).getBlock();
        return block.equals(ExampleMod.DNSSERVER_BLOCK);
    }

    public boolean isServerRouter() {
        final Block block = level.getBlockState(currentPos.above()).getBlock();
        return block.equals(ExampleMod.SERVER_BLOCK);
    }

    private record RouterNode(BlockPos pos, int depth) {
    }

    private record PathNode(BlockPos pos, int distance) {
    }

}
