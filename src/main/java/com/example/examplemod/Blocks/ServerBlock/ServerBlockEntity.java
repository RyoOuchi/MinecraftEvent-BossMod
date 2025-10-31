package com.example.examplemod.Blocks.ServerBlock;

import com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlockEntity;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Networking.ConnectionState;
import com.example.examplemod.Networking.Data.Body;
import com.example.examplemod.Networking.Data.Message;
import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Enums.ErrorCodes;
import com.example.examplemod.Networking.Data.Header;
import com.example.examplemod.Networking.Enums.Queries;
import com.example.examplemod.Networking.NetworkUtils.NetworkUtils;
import com.example.examplemod.Networking.SlidingWindow;
import com.example.examplemod.OperatingSystem.CommandInterpreter;
import com.example.examplemod.OperatingSystem.FileSystem;
import com.example.examplemod.Packet.TerminalOutputPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkDirection;
import org.lwjgl.system.CallbackI;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServerBlockEntity extends BlockEntity {
    // remember to remove when handshake is done
    final private Map<BlockPos, ConnectionState> clientSyncMap = new HashMap<>();
    final private Map<BlockPos, Map<Integer, Body>> clientSeqMap = new HashMap<>();
    final private Map<BlockPos, Integer> expectedSeqMap = new HashMap<>();
    final private Map<BlockPos, Integer> duplicateAckCount = new HashMap<>();
    final private Map<BlockPos, Boolean> endReceivedMap = new HashMap<>();
    final private Map<BlockPos, String> serverResponseMap = new HashMap<>();
    final private Map<BlockPos, Integer> lastAckNumberMap = new HashMap<>();
    final private Map<BlockPos, Boolean> receivePhaseMap = new HashMap<>();

    // file system implementation
    private final FileSystem fileSystem = new FileSystem();
    private final CommandInterpreter interpreter = new CommandInterpreter(fileSystem, this);

    // Transmission management
    private boolean readyToTransmit = false;
    private DataPacket currentPacket = null;
    private final Queue<TransmissionTask> transmissionQueue = new LinkedList<>();

    // Per-client sliding window for data transfer
    private final Map<BlockPos, SlidingWindow> slidingWindowMap = new HashMap<>();

    private static class TransmissionTask {
        final DataPacket packet;
        final WifiRouterBlockEntity target;

        TransmissionTask(DataPacket packet, WifiRouterBlockEntity target) {
            this.packet = packet;
            this.target = target;
        }
    }

    public ServerBlockEntity(final BlockPos pWorldPosition, final BlockState pBlockState) {
        super(ExampleMod.SERVER_BLOCK_ENTITY, pWorldPosition, pBlockState);
    }

    public void tickServer() {
        if (level == null || level.isClientSide()) return;

        if (!readyToTransmit && !transmissionQueue.isEmpty()) {
            TransmissionTask nextTask = transmissionQueue.poll();

            if (nextTask != null) {
                System.out.println("📡 [Server] Attempting transmission to router at " + nextTask.target.getBlockPos());
                nextTask.target.transmitPacket(nextTask.packet);

                readyToTransmit = true;
                currentPacket = nextTask.packet;

                System.out.println("🚀 [Server] Started transmission for packet: " + currentPacket);
            }
        }


        if (readyToTransmit) {
            System.out.println("✅ [Server] Transmission complete for packet: " + currentPacket);
            resetTransmissionState();
        }

        slidingWindowMap.forEach(((blockPos, slidingWindow) -> {
            if (slidingWindow == null) {
                System.out.println("⚠️ [Server] No sliding window found for current client. Handshake may not be complete.");
                return;
            }
            final List<DataPacket> timedOutPackets = slidingWindow.getTimedOutPackets();
            if (timedOutPackets.isEmpty()) {
                return;
            }
            System.out.println("⏰ [Server] Detected " + timedOutPackets.size() + " timed-out packet(s). Retransmitting...");
            System.out.println("Timed-out packets details:");
            timedOutPackets.forEach(timedOutPacket -> {
                System.out.println("   - SEQ=" + new Message(timedOutPacket.getData()).constructMessage());
            });

            retransmitTimedOutPackets(getRouterBelowServer(), timedOutPackets);
            System.out.println("🚀 [Server] Retransmitted " + timedOutPackets.size() + " timed-out packet(s).");
        }));

    }

    private void resetTransmissionState() {
        readyToTransmit = false;
        currentPacket = null;
    }

    private void queueTransmission(final DataPacket packet, final WifiRouterBlockEntity router) {
        if (packet == null || router == null) return;
        transmissionQueue.add(new TransmissionTask(packet, router));
        System.out.println("🕒 [Server] Queued transmission for packet to router at " + router.getBlockPos());
    }

    // Handle incoming data packets
    public void receiveDataPacket(final DataPacket packet) {
        System.out.println("\n🖥️ [Server] Received packet at " + worldPosition);

        final byte[] data = packet.getData();
        final String message = new String(data, StandardCharsets.UTF_8);
        System.out.println("   • Data: " + message);

        final Header header = new Header(data);
        final String headerString = header.getHeader();
        System.out.println("   • Extracted header: " + headerString);

        final int clientSynNumber = header.getSeqNumber();
        System.out.println("   • Extracted SEQ number: " + clientSynNumber);

        final int packetAckNumber = header.getAckNumber();
        System.out.println("   • Extracted ACK number: " + packetAckNumber);

        final Queries connectionStateQuery = packet.getQueryType();

        switch (connectionStateQuery) {
            case TCP_HANDSHAKE -> handleHandshake(packet, clientSynNumber, packetAckNumber);
            case TCP_ESTABLISHED -> handleDataTransfer(packet);
            case TCP_DISCONNECT -> handleDisconnect(packet);
            default -> System.out.println("   ❌ Unknown query type received: " + connectionStateQuery + "\n");
        }
    }


    private void handleDisconnect(final DataPacket packet) {
        // Disconnect logic can be expanded here if needed
        System.out.println("   • Handling disconnect from client at " + packet.getClientBlockPos() + "\n");

        final BlockPos clientPos = packet.getClientBlockPos();

        clientSyncMap.remove(clientPos);
        expectedSeqMap.remove(clientPos);
        slidingWindowMap.remove(clientPos);
        clientSeqMap.remove(clientPos);
        duplicateAckCount.remove(clientPos);
        endReceivedMap.remove(clientPos);
        serverResponseMap.remove(clientPos);
        receivePhaseMap.remove(clientPos);
        lastAckNumberMap.remove(clientPos);
        System.out.println("✅ [Server] Cleaned up connection state for client at " + clientPos + "\n");

        final DataPacket responsePacket = packet
                .invertRouterPath()
                .swapSenderAndReceiver()
                .updateErrorCode(ErrorCodes.NOERROR);

        queueTransmission(responsePacket, getRouterBelowServer());
    }

    private void handleDataTransfer(final DataPacket packet) {
        final BlockPos clientPos = packet.getClientBlockPos();
        if (!clientSyncMap.containsKey(clientPos)) {
            System.out.println("❌ [Server] Received data from unknown client: " + clientPos);
            return;
        }

        final boolean receivePhase = receivePhaseMap.getOrDefault(clientPos, true);

        if (receivePhase) handleReceivePhase(packet, clientPos);
        else handleSendPhase(packet, clientPos);
    }

    private void handleReceivePhase(final DataPacket packet, final BlockPos clientPos) {
        System.out.println("[Server] In receive phase for client " + clientPos);

        final byte[] data = packet.getData();
        final Message message = new Message(data);
        final Header header = message.getHeader();
        final int seq = header.getSeqNumber();
        final int ack = header.getAckNumber();
        final int end = header.getEndNumber();

        if (shouldDropPacket(0.2)) {
            System.out.println("🚫 [Server] Simulated packet loss — dropping data: "
                    + new Message(packet.getData()).constructMessage());
            return;
        }

        final Body body = message.getBody();
        final String payload = body.extractBody();
        final int payloadLength = payload.getBytes(StandardCharsets.UTF_8).length;

        System.out.println("\n📩 [Server] Received TCP segment from " + clientPos);
        System.out.println("   • SEQ=" + seq + ", ACK=" + ack + ", END=" + end);
        System.out.println("   • Payload: " + payload);

        initializeClientStateIfNeeded(clientPos);
        
        final Map<Integer, Body> delivered = clientSeqMap.get(clientPos);
        final int expectedSeq = expectedSeqMap.get(clientPos);

        handlePacketProcessing(seq, body, delivered, expectedSeq, clientPos, payloadLength);
        
        sendAckResponse(packet, expectedSeqMap.get(clientPos), delivered, end, clientPos);
    }

    private void handleSendPhase(final DataPacket packet, final BlockPos clientPos) {
        System.out.println("[Server] In send phase for client " + clientPos + " — ignoring received data packet.");

        final byte[] data = packet.getData();
        final Message message = new Message(data);
        final Header header = message.getHeader();
        final int ack = header.getAckNumber();

        handleDuplicateAcks(ack, clientPos, packet);
        
        final SlidingWindow slidingWindow = slidingWindowMap.get(clientPos);
        slidingWindow.acknowledge(ack);

        logSlidingWindowStatus(slidingWindow, ack);
        
        if (slidingWindow.getUnacknowledgedPackets().size() < slidingWindow.getWindowSize()) {
            System.out.println("🪶 [SERVER] Window has space. Sending next packet(s)...");
            sendPacketWithSlidingWindow(getRouterBelowServer(), slidingWindow);
        } else {
            System.out.println("🚫 [SERVER] Window full. Waiting for more ACKs.");
        }

        if (slidingWindow.getUnacknowledgedPackets().size() == 0) {
            System.out.println("✅ [SERVER] All data packets acknowledged by server.");
        }
    }

    private void initializeClientStateIfNeeded(final BlockPos clientPos) {
        final ConnectionState state = clientSyncMap.get(clientPos);
        clientSeqMap.putIfAbsent(clientPos, new TreeMap<>());
        expectedSeqMap.putIfAbsent(clientPos, state.getServerSeq());
        duplicateAckCount.putIfAbsent(clientPos, 0);
    }

    private void handlePacketProcessing(final int seq, final Body body, final Map<Integer, Body> delivered, final int expectedSeq, final BlockPos clientPos, final int payloadLength) {
        final boolean alreadyDelivered = delivered.containsKey(seq);
        final boolean inOrder = (seq == expectedSeq);
        final boolean futurePacket = (seq > expectedSeq);

        if (alreadyDelivered) System.out.println("♻️ [Server] Duplicate packet SEQ=" + seq + " (already delivered). Resending ACK.");
        else if (futurePacket) handleOutOfOrderPacket(seq, body, delivered, expectedSeq, clientPos);
        else if (inOrder) handleInOrderPacket(seq, body, delivered, expectedSeq, clientPos, payloadLength);
    }

    private void handleOutOfOrderPacket(final int seq, final Body body, final Map<Integer, Body> delivered, final int expectedSeq, final BlockPos clientPos) {
        System.out.println("⚠️ [Server] Out-of-order packet! Expected " + expectedSeq + " but got " + seq);
        delivered.put(seq, body);
        
        final int dupCount = duplicateAckCount.getOrDefault(clientPos, 0) + 1;
        duplicateAckCount.put(clientPos, dupCount);
        if (dupCount >= 1) System.out.println("🔁 [Server] Sending duplicate ACK #" + dupCount + " for missing SEQ=" + expectedSeq);
    }

    private void handleInOrderPacket(final int seq, final Body body, final Map<Integer, Body> delivered, int expectedSeq, final BlockPos clientPos, final int payloadLength) {
        System.out.println("🧩 [Server] In-order packet. Delivering SEQ=" + seq);
        delivered.put(seq, body);
        expectedSeq += payloadLength;

        advanceExpectedSeq(delivered, expectedSeq, clientPos);
    }

    private void advanceExpectedSeq(final Map<Integer, Body> delivered, int expectedSeq, final BlockPos clientPos) {
        boolean advanced = true;
        while (advanced) {
            advanced = false;
            for (final Iterator<Map.Entry<Integer, Body>> it = delivered.entrySet().iterator(); it.hasNext();) {
                final Map.Entry<Integer, Body> entry = it.next();
                final int bufferedSeq = entry.getKey();
                if (bufferedSeq == expectedSeq) {
                    final Body bufferedBody = entry.getValue();
                    expectedSeq += bufferedBody.extractBody().getBytes(StandardCharsets.UTF_8).length;
                    advanced = true;
                }
            }
        }

        expectedSeqMap.put(clientPos, expectedSeq);
        duplicateAckCount.put(clientPos, 0);

        System.out.println("✅ [Server] Updated expectedSeq=" + expectedSeq);
    }

    private void handleDuplicateAcks(final int ack, final BlockPos clientPos, final DataPacket packet) {
        final int lastAckNumber = lastAckNumberMap.getOrDefault(clientPos, -1);
        final int clientDuplicateAckCount = duplicateAckCount.getOrDefault(clientPos, 0);
        
        if (ack == lastAckNumber) {
            duplicateAckCount.put(clientPos, clientDuplicateAckCount + 1);
            System.out.println("🔁 [Server] Duplicate ACK #" + duplicateAckCount + " detected for " + ack);

            if (clientDuplicateAckCount >= 1) {
                System.out.println("⚡ [Server] duplicate ACKs detected → Performing FAST RETRANSMIT!");
                fastRetransmit(getRouterBelowServer(), ack, clientPos, packet);
                duplicateAckCount.put(clientPos, 0);
            }
        } else {
            duplicateAckCount.put(clientPos, 0);
            lastAckNumberMap.put(clientPos, ack);
        }
    }

    private void logSlidingWindowStatus(final SlidingWindow slidingWindow, final int ack) {
        final int unackedCount = slidingWindow.getUnacknowledgedPackets().size();
        System.out.println("   • Updated sliding window with ACK=" + ack);
        System.out.println("   • Last Acked SEQ in window: " + slidingWindow.getLastAckedSeq());
        System.out.println("   • Unacknowledged packets in window: " + unackedCount);
        System.out.println("   • Unacknowledged packets details:");
        slidingWindow.getUnacknowledgedPackets().forEach(unacknowledgedPacket -> {
            System.out.println("      - SEQ=" + new Header(unacknowledgedPacket.getData()).getSeqNumber());
            System.out.println("      - Data: " + new Message(unacknowledgedPacket.getData()).getBody().extractBody());
        });
    }

    private void sendAckResponse(final DataPacket packet, final int expectedSeq, final Map<Integer, Body> delivered,
                                final int end, final BlockPos clientPos) {
        final byte[] ackBytes = NetworkUtils.createAckPacket(expectedSeq);

        final DataPacket ackPacket = packet
                .swapSenderAndReceiver()
                .invertRouterPath()
                .updateQueryType(Queries.TCP_ESTABLISHED)
                .updateData(ackBytes)
                .updateErrorCode(ErrorCodes.NOERROR);

        final BlockPos routerPos = worldPosition.below();
        final BlockEntity routerEntity = level.getBlockEntity(routerPos);

        if (end == 1) {
            endReceivedMap.put(clientPos, true);
            System.out.println("📩 [Server] Marked END received for client " + clientPos);
        }

        if (endReceivedMap.getOrDefault(clientPos, false) && isDeliveredDataContiguous(delivered)) {
            handleCompleteDataTransfer(ackPacket, delivered, clientPos, packet, routerEntity);
        } else {
            System.out.println("⚠️ [Server] Final segment received but data not fully contiguous — still waiting for missing packets.");
            final String reconstructedData = NetworkUtils.reconstructData(delivered);
            System.out.println("🔄 [Server] Reconstructed data so far: '" + reconstructedData + "'\n");
        }

        if (routerEntity instanceof WifiRouterBlockEntity router) {
            queueTransmission(ackPacket, router);
            System.out.println("✅ [Server] ACK sent (ACK=" + expectedSeq + ") to client " + clientPos);
        }

        // Debug: show all delivered packets
        System.out.println("🗂️ [Server] Delivered data buffer for client " + clientPos + ":");
        delivered.forEach((seqNum, bodyObj) ->
                System.out.println("   • SEQ=" + seqNum + " | Data='" + bodyObj.extractBody() + "'"));
        final String reconstructedData = NetworkUtils.reconstructData(delivered);
        System.out.println("🔄 [Server] Reconstructed data so far: '" + reconstructedData + "'\n");
    }

    private void handleCompleteDataTransfer(final DataPacket ackPacket, final Map<Integer, Body> delivered,
                                           final BlockPos clientPos, final DataPacket packet, final BlockEntity routerEntity) {
        final String reconstructedData = NetworkUtils.reconstructData(delivered);
        System.out.println("🏁 [Server] Final segment received and all data contiguous — complete transmission!");
        System.out.println("🎉 [Server] Full reconstructed data from client " + clientPos + ": '" + reconstructedData);
        System.out.println("📂 [Server] Attempting to read file at path: " + reconstructedData + "'\n");

        final String reconstructedFilePath = reconstructedData.contains("/")
                ? reconstructedData.substring(reconstructedData.indexOf("/") + 1)
                : reconstructedData;

        System.out.println("[Server] Reconstructed file path: " + reconstructedFilePath);

        final String fileContents = fileSystem.cat(reconstructedFilePath)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .orElse(null);
                
        if (fileContents == null) {
            handleFileNotFound(ackPacket, expectedSeqMap.get(clientPos), clientPos, routerEntity);
            return;
        }
        
        System.out.println("--- File Content Start ---");
        System.out.println(fileContents);
        System.out.println("--- File Content End ---\n");
        serverResponseMap.putIfAbsent(clientPos, fileContents);

        System.out.println("[Server] In send phase for client " + clientPos);
        final SlidingWindow window = slidingWindowMap.get(packet.getClientBlockPos());

        final BlockEntity blockEntity = level.getBlockEntity(worldPosition.below());
        if (!(blockEntity instanceof WifiRouterBlockEntity routerBlockEntity)) {
            System.out.println("❌ [Server] No WifiRouter found below the server at: " + worldPosition.below());
            return;
        }
        receivePhaseMap.put(clientPos, false);

        sendFileData(routerBlockEntity, packet, serverResponseMap.get(clientPos).getBytes(StandardCharsets.UTF_8), window);
    }

    private void handleFileNotFound(final DataPacket ackPacket, final int expectedSeq, final BlockPos clientPos, final BlockEntity routerEntity) {
        System.out.println("❌ [Server] File not found at path\n");
        final DataPacket noFilePacket = ackPacket.updateQueryType(Queries.TCP_DISCONNECT);

        if (routerEntity instanceof WifiRouterBlockEntity router) {
            queueTransmission(noFilePacket, router);
            System.out.println("✅ [Server] ACK sent (ACK=" + expectedSeq + ") to client " + clientPos);
        }
    }



    private void fastRetransmit(final WifiRouterBlockEntity responseRouter, final int ackNumber, final BlockPos clientPos, final DataPacket originalPacket) {
        final String serverResponse = serverResponseMap.get(clientPos);
        final byte[] fileData = serverResponse != null ? serverResponse.getBytes(StandardCharsets.UTF_8) : null;
        if (fileData == null || fileData.length == 0) {
            System.out.println("⚠️ [Server] No URL data to reconstruct missing packet.");
            return;
        }

        final ConnectionState connectionState = clientSyncMap.get(clientPos);

        // Each SEQ corresponds to a byte offset within fileData
        final int baseSeq = connectionState.getServerSeq();
        final int offset = ackNumber - baseSeq; // how far into fileData the missing data starts
        final int chunkSize = SlidingWindow.MAX_DATA_SIZE; // same as in sendUrlData

        if (offset < 0 || offset >= fileData.length) {
            System.out.println("⚠️ [Server] Invalid ACK offset for retransmission: " + offset);
            return;
        }

        final int end = Math.min(fileData.length, offset + chunkSize);
        final byte[] chunk = new byte[end - offset];
        System.arraycopy(fileData, offset, chunk, 0, end - offset);

        final boolean isLastChunk = (end == fileData.length);

        // Rebuild header and message exactly as original send
        final byte[] packetBytes = NetworkUtils.createTcpPacket(
                ackNumber,
                connectionState.getServerSeq(),
                1, // response number
                isLastChunk,
                chunk
        );


        System.out.println("⚡ [Server] FAST RETRANSMIT triggered for SEQ=" + ackNumber +
                " | Chunk='" + new String(chunk, StandardCharsets.UTF_8) + "'");

        final DataPacket updatedPacket = originalPacket
                .swapSenderAndReceiver()
                .invertRouterPath()
                .updateQueryType(Queries.TCP_ESTABLISHED)
                .updateData(packetBytes)
                .updateErrorCode(ErrorCodes.NOERROR);

        queueTransmission(updatedPacket, responseRouter);
    }

    @Nullable
    private WifiRouterBlockEntity getRouterBelowServer() {
        BlockPos routerPos = worldPosition.below();
        BlockEntity routerEntity = level.getBlockEntity(routerPos);
        if (!(routerEntity instanceof WifiRouterBlockEntity router)) {
            System.out.println("❌ [Server] No WifiRouter found below the server at: " + routerPos);
            return null;
        }
        return router;
    }

    private void sendFileData(final WifiRouterBlockEntity responseRouter, final DataPacket clientRequestPacket, final byte[] fileContents, final SlidingWindow window) {
        System.out.println("\n📤 [Server] Preparing to send file data to client at " + clientRequestPacket.getClientBlockPos());

        if (fileContents == null || fileContents.length == 0) {
            System.out.println("❌ [Server] No file contents to send.");
            return;
        }

        if (window == null) {
            System.out.println("❌ [Server] Sliding window not initialized for client at " + clientRequestPacket.getClientBlockPos());
            return;
        }

        final ConnectionState connectionState = clientSyncMap.get(clientRequestPacket.getClientBlockPos());

        final int chunkSize = SlidingWindow.MAX_DATA_SIZE;
        final int baseSeq = connectionState.getServerSeq();
        final int ackBase = connectionState.getClientSeq();

        System.out.println("🌐 [Server] Sending file data to browser at " + clientRequestPacket.getClientBlockPos() + " | Starting SEQ=" + baseSeq + " | ACK=" + ackBase);

        for (int i = 0; i < fileContents.length; i += chunkSize) {
            final int end = Math.min(fileContents.length, i + chunkSize);
            final byte[] chunk = new byte[end - i];
            System.arraycopy(fileContents, i, chunk, 0, end - i);

            final int seq = baseSeq + i;

            final boolean isLastChunk = (end == fileContents.length);

            final byte[] packetBytes = NetworkUtils.createTcpPacket(
                    seq,
                    ackBase,
                    1, // response number
                    isLastChunk,
                    chunk
            );

            // Construct DataPacket with all routing and addressing info
            final DataPacket packet = clientRequestPacket
                    .updateQueryType(Queries.TCP_ESTABLISHED)
                    .updateData(packetBytes)
                    .updateErrorCode(ErrorCodes.NOERROR)
                    .invertRouterPath()
                    .swapSenderAndReceiver();

            final List<BlockPos> cablePath = NetworkUtils.getCablePathToNextRouter(level, packet.getRouterPath(), packet.getRouterPath().get(0));

            final DataPacket finalPacket = packet.updateCablePath(cablePath);

            window.queueData(finalPacket);
        }
        sendPacketWithSlidingWindow(responseRouter, window);
    }

    private void sendPacketWithSlidingWindow(final WifiRouterBlockEntity responseRouter, final SlidingWindow slidingWindow) {
        if (slidingWindow == null) {
            System.out.println("⚠️ [Server] Sliding window not initialized. Handshake may not be complete.");
            return;
        }

        final List<DataPacket> packetsToSend = slidingWindow.getPacketsToSend();

        for (final DataPacket packet : packetsToSend) {
//            responseRouter.performServerRequest(
//                    packet.getData(),
//                    clientIPAddress,
//                    null,
//                    worldPosition,
//                    packet.getQueryType()
//            );

            queueTransmission(packet, responseRouter);
            final Header header = new Header(packet.getData());
            final int seqNum = header.getSeqNumber();
            System.out.println("🚀 [Server] Sent data packet with SEQ=" + seqNum);
        }
    }

    private boolean isDeliveredDataContiguous(final Map<Integer, Body> deliveredMap) {
        return NetworkUtils.isDataContiguous(deliveredMap);
    }

    private void retransmitTimedOutPackets(final WifiRouterBlockEntity responseRouter, final List<DataPacket> timedOutPackets) {
        for (final DataPacket packet : timedOutPackets) {
            queueTransmission(packet, responseRouter);

            final Header header = new Header(packet.getData());
            final int seqNum = header.getSeqNumber();
            System.out.println("🚀 [Browser] Retransmitted timed-out data packet with SEQ=" + seqNum);
        }
    }

    // Function for testing and simulating packet loss
    // lossProbability = 0.1 → 10% packet loss
    private boolean shouldDropPacket(double lossProbability) {
        return Math.random() < lossProbability;
    }



    private void handleHandshake(final DataPacket packet, final int clientSynNumber, final int clientAckNumber) {
        // Handshake logic can be expanded here if needed
        if (clientAckNumber == -1) {
            // if ack number from client doesnt exist
            // first transmission from client to server

            // Build response header
            final int ackNumber = clientSynNumber + 1;
            final int serverSynNumber = new Random().nextInt(100000);

            // Track connection state
            if (!clientSyncMap.containsKey(packet.getClientBlockPos())) {
                clientSyncMap.put(packet.getClientBlockPos(), new ConnectionState(ackNumber, serverSynNumber));
            }

            System.out.println("   • Responding with SEQ=" + serverSynNumber + " and ACK=" + ackNumber + " to " + packet.getClientBlockPos() + "\n");

            final Map<String, String> responseHeaderMap = Map.of(
                    "SEQ", Integer.toString(serverSynNumber),
                    "ACK", Integer.toString(ackNumber)
            );

            final Header responseHeader = new Header(responseHeaderMap);
            final byte[] responseData = responseHeader.getHeader().getBytes(StandardCharsets.UTF_8);

            // Build response packet
            final DataPacket responsePacket = packet
                    .swapSenderAndReceiver()
                    .invertRouterPath()
                    .updateErrorCode(ErrorCodes.NOERROR)
                    .updateData(responseData);

            // Determine router directly below this server
            final BlockPos routerPos = worldPosition.below();
            final BlockEntity routerEntity = level.getBlockEntity(routerPos);
            if (!(routerEntity instanceof WifiRouterBlockEntity router)) {
                System.out.println("❌ [Server] No WifiRouter found below the server at: " + routerPos);
                return;
            }

            // Queue the response to be sent to the router
            queueTransmission(responsePacket, router);
        } else if (clientSynNumber == -1) {
            // if syn number from client doesnt exist
            // second transmission from client to server -> last step in handshake
            final ConnectionState clientSavedConnectionData = clientSyncMap.get(packet.getClientBlockPos());
            if (!clientSavedConnectionData.validateServerAckNumber(clientAckNumber)) {
                System.out.println("   ❌ Handshake failed with client at " + packet.getClientBlockPos() + "\n");
                return;
            }

            System.out.println("   • Handshake complete with client at " + packet.getClientBlockPos() + "\n");
            System.out.println("Commencing data transfer phase...\n");

            ConnectionState state = clientSyncMap.get(packet.getClientBlockPos());
            ConnectionState updatedState = state
                    .updateServerSeq(state.serverSeq() + 1);
            clientSyncMap.put(packet.getClientBlockPos(), updatedState);

            // --- Initialize per-client sequence tracking for data transfer ---
            final BlockPos clientPos = packet.getClientBlockPos();

            // The first data packet from the client should start right after its initial SYN
            // So expectedSeq = client's ACK number (the next byte expected)
            int initialExpectedSeq = updatedState.getClientSeq();

            expectedSeqMap.put(clientPos, initialExpectedSeq);
            duplicateAckCount.put(clientPos, 0);
            clientSeqMap.putIfAbsent(clientPos, new TreeMap<>());
            receivePhaseMap.putIfAbsent(clientPos, true);
            System.out.println("📥 [Server] Initialized expectedSeqMap for client " + clientPos +
                    " | Starting expected SEQ=" + initialExpectedSeq);

            final int currentServerSynNumber = clientSyncMap.get(packet.getClientBlockPos()).serverSeq();

            System.out.println("Current Server SYN Number for client at " + packet.getClientBlockPos() + ": " + currentServerSynNumber + "\n");

            final SlidingWindow window = new SlidingWindow(updatedState.getServerSeq(), SlidingWindow.DEFAULT_WINDOW_SIZE);
            slidingWindowMap.put(packet.getClientBlockPos(), window);

            System.out.println("🧮 [Server] Initialized sliding window for client " + packet.getClientBlockPos()
                    + " base seq=" + updatedState.getServerSeq());

        }
    }


    // Implemented command to use terminal outputs. Need to convert sout values to string.
    public String executeCommand(final String command) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try (PrintStream ps = new PrintStream(buffer)) {
            System.setOut(ps);
            interpreter.execute(command);
            this.setChanged();
        } finally {
            System.setOut(originalOut);
        }
        return buffer.toString().trim();
    }

    public void sendTerminalOutputToClient(final ServerPlayer player, final String output) {
        String currentPath = interpreter.getContext().getCurrentDirectory().getFullPath();
        ExampleMod.CHANNEL.sendTo(
                new TerminalOutputPacket(output, currentPath),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        // Serialize FileSystem and store inside tag
        CompoundTag fsTag = fileSystem.saveToNBT();
        tag.put("VirtualFileSystem", fsTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        // Deserialize FileSystem when world loads
        if (tag.contains("VirtualFileSystem")) {
            CompoundTag fsTag = tag.getCompound("VirtualFileSystem");
            fileSystem.loadFromNBT(fsTag);
        }
    }
}
