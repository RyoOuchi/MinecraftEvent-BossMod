package com.example.examplemod.Blocks.DNSServerBlock;

import com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlockEntity;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Enums.ErrorCodes;
import com.example.examplemod.Networking.ErrorCode.ResponseHandler;
import com.example.examplemod.ServerData.ServerSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlockEntity.findCablePathBetweenRouters;

public class DNSServerBlockEntity extends BlockEntity {
    private DataPacket dataPacket;
    private final BlockPos currentPos;
    private final Map<String, BlockPos> dnsDomainToIPMap = new HashMap<>();

    // state variables
    private boolean readyToTransmitData = false;
    private DataPacket packetToTransmit = null;

    public DNSServerBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ExampleMod.DNSSERVER_BLOCK_ENTITY, pWorldPosition, pBlockState);
        this.currentPos = pWorldPosition;

    }

    public void receiveDataPacket(final DataPacket packet) {
        if (packet == null) {
            System.out.println("⚠️ Received null DataPacket at " + currentPos);
            return;
        }

        this.dataPacket = packet;

        // Load domain → IP map from ServerSavedData
        populateDnsMap();

        System.out.println("\n📡 [DNS Server] Received packet at " + currentPos);
        System.out.println("   • Router Path:");
        packet.getRouterPath().forEach(pos -> System.out.println("     → " + pos));

        // Debug: mark visually that this server received a request
        level.setBlockAndUpdate(currentPos.above(4), Blocks.OBSIDIAN.defaultBlockState());

        // Extract URL from packet
        final String requestedUrl = new String(packet.getData());
        final String extractedDomain = extractDomainFromUrl(requestedUrl);

        System.out.println("🌐 Processing DNS Request:");
        System.out.println("   • Full URL: " + requestedUrl);
        System.out.println("   • Extracted Domain: " + extractedDomain);

        // Validate and determine error code
        final ErrorCodes resultCode = validateDataPacket(extractedDomain);
        final BlockPos resolvedIP = dnsDomainToIPMap.get(extractedDomain);

        // Logging resolution outcome
        switch (resultCode) {
            case NOERROR -> System.out.println("✅ Domain resolved: " + extractedDomain + " → " + resolvedIP);
            case NXDOMAIN -> System.out.println("❌ Domain not found: " + extractedDomain);
            case FORMERR -> System.out.println("⚠️ Malformed DNS request for: " + requestedUrl);
        }

        // Build response packet
        final DataPacket responsePacket = buildResponsePacket(packet, resolvedIP, resultCode);

        System.out.println("📤 Generated DNS Response Packet with code: " + resultCode);
        System.out.println("   • Router Path for Response:");
        responsePacket.getRouterPath().forEach(pos -> System.out.println("     → " + pos));
        readyToTransmitData = true;
        packetToTransmit = responsePacket;
        dataPacket = null;
    }

    private DataPacket buildResponsePacket(final DataPacket packet, final BlockPos resolvedIP, final ErrorCodes errorCode) {
        if (packet == null) return null;

        // Convert BlockPos to a byte array (e.g., "x:y:z" → bytes)
        final String ipString = resolvedIP != null
                ? resolvedIP.getX() + ":" + resolvedIP.getY() + ":" + resolvedIP.getZ()
                : "0:0:0"; // fallback if null
        final byte[] ipBytes = ipString.getBytes(StandardCharsets.UTF_8);

        // Create a new DataPacket with the updated data
        final DataPacket updateDataPacket = packet.updateData(ipBytes);
        final DataPacket updateRouterPathPacket = updateDataPacket.invertRouterPath();
        final DataPacket updateSendToAndSenderBlockPosPacket = updateRouterPathPacket.swapSenderAndReceiver();

        final List<BlockPos> path = updateSendToAndSenderBlockPosPacket.getRouterPath();

        BlockPos nextRouterPos = getNextRouterBlockPos(path);

        if (nextRouterPos == null) {
            nextRouterPos = currentPos;
        }

        List<BlockPos> cableBlocks = findCablePathBetweenRouters(level, currentPos.below(), nextRouterPos);

        final DataPacket updateCablePathPacket = updateSendToAndSenderBlockPosPacket.updateCablePath(cableBlocks);
        final DataPacket finalPacket = ResponseHandler.handleResponse(errorCode, updateCablePathPacket);

        System.out.println("📦 Built response packet with resolved IP: " + ipString + " (ErrorCode: " + errorCode + ")");
        return finalPacket;
    }

    @Nullable
    private BlockPos getNextRouterBlockPos(final List<BlockPos> path) {
        if (path == null || path.isEmpty()) return null;

        // Find the index of the current router in the path
        final int currentIndex = path.indexOf(currentPos.below());

        if (currentIndex == -1) {
            System.out.println("⚠️ Current router position not found in path: " + currentPos.below());
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

    public void tickServer() {
        if (readyToTransmitData && packetToTransmit != null) {

            final BlockEntity be = level.getBlockEntity(currentPos.below());
            if (!(be instanceof WifiRouterBlockEntity wifiRouterBlockEntity)) return;
            if (!wifiRouterBlockEntity.isDNSServerRouter()) return;

            System.out.println("🚀 Transmitting DNS response packet from " + currentPos + " to router at " + be.getBlockPos());

            wifiRouterBlockEntity.transmitPacket(packetToTransmit);

            readyToTransmitData = false;
            packetToTransmit = null;
        }
    }


    private ErrorCodes validateDataPacket(final String extractedDomain) {
        if (dataPacket == null) {
            System.out.println("⚠️ Received null data packet at position: " + this.worldPosition);
            return ErrorCodes.FORMERR;
        }
        if (dataPacket.getData() == null || dataPacket.getData().length == 0) {
            return ErrorCodes.FORMERR;
        }
        if (dnsDomainToIPMap.isEmpty() || !dnsDomainToIPMap.containsKey(extractedDomain)) {
            // Domain not found in DNS map
            return ErrorCodes.NXDOMAIN;
        }

        final BlockPos resolvedIP = dnsDomainToIPMap.get(extractedDomain);

        if (resolvedIP == null) {
            System.out.println("⚠️ Could not resolve domain: " + extractedDomain);
            return ErrorCodes.NXDOMAIN;
        }

        return ErrorCodes.NOERROR;
    }


    private String extractDomainFromUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        int slashIndex = url.indexOf('/');
        if (slashIndex != -1)
            return url.substring(0, slashIndex);
        else
            return url;
    }

    private void populateDnsMap() {
        dnsDomainToIPMap.clear();
        if (!(level instanceof ServerLevel serverLevel)) return;
        final ServerSavedData data = ServerSavedData.get(serverLevel);
        final Map<String, BlockPos> servers = data.getServers();
        dnsDomainToIPMap.putAll(servers);
    }

}
