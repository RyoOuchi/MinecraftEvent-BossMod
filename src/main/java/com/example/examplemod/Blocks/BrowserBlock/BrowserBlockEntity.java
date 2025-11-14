package com.example.examplemod.Blocks.BrowserBlock;

import com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlockEntity;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Networking.ConnectionState;
import com.example.examplemod.Networking.Data.Body;
import com.example.examplemod.Networking.Data.Message;
import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Data.Header;
import com.example.examplemod.Networking.Enums.ErrorCodes;
import com.example.examplemod.Networking.Enums.Queries;
import com.example.examplemod.Networking.NetworkUtils.NetworkUtils;
import com.example.examplemod.Networking.SlidingWindow;
import com.example.examplemod.Packet.BrowserResponsePacket;
import com.example.examplemod.Screens.BrowserScreen.BrowserLoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class BrowserBlockEntity extends BlockEntity {
    // per request. Remember to reset(set to null) after each request
    private BlockPos serverIPAddress;
    private byte[] urlData;
    private int duplicateAckCount = 0;
    private int lastAckNumber = -1;

    // 受信済みデータのバッファ（SEQ → Body）。順不同・欠落再送のための一時領域
    final private Map<Integer, Body> receivedDataChunks = new TreeMap<>();
    private int expectedSeqNumber = -1;
    private boolean endFlagReceived = false;

    private SlidingWindow slidingWindow;

    private ConnectionState connectionState;

    public BrowserBlockEntity(final BlockPos pWorldPosition, final BlockState pBlockState) {
        super(ExampleMod.BROWSER_BLOCK_ENTITY, pWorldPosition, pBlockState);
    }

    /**
     * URL 文字列を UTF-8 バイト列へ変換。
     */
    public byte[] convertUrlToByte(final String url) {
        return url.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * DNS 応答を受信し、成功時はサーバ位置へ TCP ハンドシェイクを開始する。
     * エラーの種類に応じてクライアントへ通知を返す。
     */
    public void receiveDNSResponse(final DataPacket dnsResponsePacket, final WifiRouterBlockEntity responseRouter) {
        final Map<ErrorCodes, Consumer<DataPacket>> handlers = new EnumMap<>(ErrorCodes.class);

        handlers.put(ErrorCodes.NXDOMAIN, packet -> {
            System.out.println("❌ [Browser] DNS Response Error: NXDOMAIN - The domain name does not exist.");
            sendBrowserResponseToClient("❌ [Browser] DNS Response Error: NXDOMAIN - The domain name does not exist.", "");
        });

        handlers.put(ErrorCodes.FORMERR, packet -> {
            System.out.println("❌ [Browser] DNS Response Error: FORMERR - The DNS server was unable to process the query due to a format error.");
            sendBrowserResponseToClient("❌ [Browser] DNS Response Error: FORMERR - The DNS server was unable to process the query due to a format error.", "");
        });

        handlers.put(ErrorCodes.NOERROR, packet -> {
            final byte[] ipBytes = packet.getData();
            this.serverIPAddress = convertBytesToBlockPos(ipBytes);
            System.out.println("✅ [Browser] DNS Response Success: Resolved to IP " + serverIPAddress);

            final Random random = new Random();
            final int clientSynNumber = random.nextInt(100000);

            connectionState = new ConnectionState(clientSynNumber, -1);

            final String randomSeqString = Integer.toString(clientSynNumber);

            final Map<String, String> headerMap = Map.of(
                    "SEQ", randomSeqString
            );

            final Header header = new Header(headerMap);
            System.out.println("🌐 [Browser] Sending SEQ to server at " + serverIPAddress + " with seq=" + clientSynNumber);

            final byte[] synData = header.getHeader().getBytes(StandardCharsets.UTF_8);
            // サーバへクライアント初期 SEQ を送信（SYN 相当）
            responseRouter.performServerRequest(synData, serverIPAddress, null, worldPosition, Queries.TCP_HANDSHAKE);
        });

        handlers.getOrDefault(dnsResponsePacket.getErrorCode(), packet ->
                System.out.println("⚠️ [Browser] Unhandled DNS error code: " + packet.getErrorCode())
        ).accept(dnsResponsePacket);
    }

    /**
     * サーバー側のティックで呼び出され、ウィンドウ内のタイムアウトパケットを検出・再送する。
     */
    public void tickServer() {
        if (slidingWindow == null) return;
        final List<DataPacket> timedOutPackets = slidingWindow.getTimedOutPackets();
        if (timedOutPackets.isEmpty()) {
            return;
        }
        System.out.println("⏰ [Browser] Detected " + timedOutPackets.size() + " timed-out packet(s). Retransmitting...");
        System.out.println("Timed-out packets details:");
        timedOutPackets.forEach(timedOutPacket -> {
            System.out.println("   - SEQ=" + new Message(timedOutPacket.getData()).constructMessage());
        });

        final BlockPos firstRouterPos = timedOutPackets.get(0).getRouterPath().get(0);

        final BlockEntity be = level.getBlockEntity(firstRouterPos);
        if (!(be instanceof WifiRouterBlockEntity wifiRouterBlockEntity)) {
            System.out.println("❌ [Browser] Failed to retransmit timed-out packets: First router in path is not a WifiRouterBlockEntity.");
            return;
        }

        retransmitTimedOutPackets(wifiRouterBlockEntity, timedOutPackets);
        System.out.println("🚀 [Browser] Retransmitted " + timedOutPackets.size() + " timed-out packet(s).");
    }

    /**
     * 疑似パケットロスを発生させる判定。
     * @param lossProbability ロス確率 (0.0-1.0)
     */
    private boolean shouldDropPacket(final double lossProbability) {
        return Math.random() < lossProbability;
    }

    /**
     * サーバからの SYN+ACK を処理し、ACK を返して確立状態へ移行する。
     * その後スライディングウィンドウを初期化し、URL データ送信を開始する。
     */
    public void receiveHandshakeServerResponse(final DataPacket serverResponsePacket, final WifiRouterBlockEntity responseRouter) {
        final byte[] responseData = serverResponsePacket.getData();
        final String message = new String(responseData, StandardCharsets.UTF_8);

        System.out.println("✅ [Browser] Server Response Success: " + message);

        final Header header = new Header(responseData);
        final int ackNumber = header.getAckNumber();

        final boolean isValidAck = connectionState.validateClientAckNumber(ackNumber);

        if (!isValidAck) {
            System.out.println("❌ [Browser] Invalid ACK number received: " + ackNumber + ". Expected: " + (connectionState.getClientSeq() + 1));
            return;
        }

        final int serverSynNumber = header.getSeqNumber();

        final Header responseHeader = new Header(Map.of(
                "ACK", Integer.toString(serverSynNumber + 1)
        ));

        final byte[] ackData = responseHeader.getHeader().getBytes(StandardCharsets.UTF_8);

        System.out.println("🌐 [Browser] Sending ACK to server at " + serverIPAddress + " with ack=" + (serverSynNumber + 1));

        responseRouter.performServerRequest(ackData, serverIPAddress, null, worldPosition, Queries.TCP_HANDSHAKE);

        connectionState = connectionState
                .updateServerSeq(serverSynNumber + 1)
                .updateClientSeq(ackNumber);

        // クライアントの現在 SEQ を基準に送信ウィンドウを初期化
        slidingWindow = new SlidingWindow(connectionState.getClientSeq(), SlidingWindow.DEFAULT_WINDOW_SIZE);
        System.out.println("🤝 [Browser] Handshake complete. Entering ESTABLISHED state.");
        sendUrlData(responseRouter, serverResponsePacket);
        expectedSeqNumber = connectionState.getServerSeq();
    }




    /**
     * サーバからの DISCONNECT 応答受信時に、接続状態と一時データをクリアする。
     */
    public void receiveDisconnectServerResponse(final DataPacket packet, final WifiRouterBlockEntity blockEntity){
        System.out.println("✅ [Browser] Received DISCONNECT response from server.");
        serverIPAddress = null;
        urlData = null;
        slidingWindow = null;
        connectionState = null;
        duplicateAckCount = 0;
        lastAckNumber = -1;
        receivedDataChunks.clear();
        expectedSeqNumber = -1;
        endFlagReceived = false;
    }

    /**
     * 確立状態でのデータ/ACK の受信を処理する。
     * responseNumber=1 ならデータセグメント、そうでなければ ACK セグメントとして処理。
     */
    public void receiveEstablishedServerResponse(final DataPacket packet, final WifiRouterBlockEntity blockEntity) {
        System.out.println("✅ [Browser] Received ESTABLISHED response from server.");
        System.out.println("\n📦 [Browser] Data Packet Details: " + new Message(packet.getData()).constructMessage());
        final byte[] responseData = packet.getData();
        final Message message = new Message(responseData);
        final Header header = message.getHeader();
        final int responseNumber = header.getResponseNumber();

        final EstablishedContext ctx = new EstablishedContext(packet, blockEntity, header, message);

        if (responseNumber == 1) handleEstablishedDataSegment(ctx);
        else handleEstablishedAckSegment(ctx);
    }

    /**
     * ESTABLISHED 処理時に使う文脈オブジェクト。
     */
    private record EstablishedContext(DataPacket packet, WifiRouterBlockEntity router, Header header, Message message) {
    }

    private void handleEstablishedDataSegment(final EstablishedContext ctx) {
        final int ackNumber = ctx.header.getAckNumber();
        final int seq = ctx.header.getSeqNumber();
        final int endFlag = ctx.header.getEndNumber();

        if (shouldDropPacket(0.2)) {
            System.out.println("🚫 [Browser] Simulated packet loss — dropping data: " + new Message(ctx.packet.getData()).constructMessage());
            return;
        }

        System.out.println("✅ [Browser] Server indicates file found. Preparing to receive data...");
        final Body messageBody = ctx.message.getBody();
        final String payload = messageBody.extractBody();
        final int payloadLength = payload.getBytes(StandardCharsets.UTF_8).length;

        System.out.println("\n📩 [Browser] Received TCP segment from " + serverIPAddress);
        System.out.println("   • SEQ=" + seq + ", ACK=" + ackNumber);
        System.out.println("   • Payload: " + payload);

        duplicateAckCount = 0;

        final boolean alreadyReceived = receivedDataChunks.containsKey(seq);
        final boolean inOrder = (seq == expectedSeqNumber);
        final boolean futurePacket = (seq > expectedSeqNumber);

        if (alreadyReceived) {
            System.out.println("🔁 [Browser] Duplicate packet detected for SEQ=" + seq + ". Ignoring payload.");
        } else if (inOrder) {
            receivedDataChunks.put(seq, ctx.message.getBody());
            expectedSeqNumber += payloadLength;
            System.out.println("✅ [Browser] In-order packet received for SEQ=" + seq + ". Updated expected SEQ to " + expectedSeqNumber);

            boolean advanced = true;
            while (advanced) {
                advanced = false;
                for (final Iterator<Map.Entry<Integer, Body>> it = receivedDataChunks.entrySet().iterator(); it.hasNext();) {
                    final Map.Entry<Integer, Body> entry = it.next();
                    final int bufferedSeq = entry.getKey();
                    if (bufferedSeq == expectedSeqNumber) {
                        final Body bufferedBody = entry.getValue();
                        expectedSeqNumber += bufferedBody.extractBody().getBytes(StandardCharsets.UTF_8).length;
                        advanced = true;
                    }
                }
            }
            duplicateAckCount = 0;
            System.out.println("   • Updated expected SEQ to " + expectedSeqNumber);

        } else if (futurePacket) {
            receivedDataChunks.put(seq, ctx.message.getBody());
            System.out.println("📦 [Browser] Out-of-order packet received for SEQ=" + seq + ". Stored for later reassembly. Current expected SEQ is " + expectedSeqNumber);
            duplicateAckCount = duplicateAckCount + 1;
            if (duplicateAckCount >= 1) {
                System.out.println("🔁 [Server] Sending duplicate ACK #" + duplicateAckCount + " for missing SEQ=" + expectedSeqNumber);
            }
        }

        final byte[] ackBytes = NetworkUtils.createAckPacket(expectedSeqNumber);

        ctx.router.performServerRequest(ackBytes, serverIPAddress, null, worldPosition, Queries.TCP_ESTABLISHED);
        System.out.println("🗂️ [Client] Delivered data buffer for server " + serverIPAddress + ":");
        receivedDataChunks.forEach((seqNum, bodyObj) ->
                System.out.println("   • SEQ=" + seqNum + " | Data='" + bodyObj.extractBody() + "'"));
        final String reconstructedData = NetworkUtils.reconstructData(receivedDataChunks);

        System.out.println("🔄 [Browser] Reconstructed data so far: '" + reconstructedData + "'\n");

        if (endFlag == 1) {
            System.out.println("✅ [Browser] Received END flag from server. Final Packet Received.");
            endFlagReceived = true;
        }

        if (endFlagReceived && isDeliveredDataContiguous(receivedDataChunks)) {
            System.out.println("🎉 [Browser] All data packets received and reconstructed successfully from server at " + serverIPAddress);
            System.out.println("   • Final Reconstructed Data: '" + reconstructedData + "'");

            final Header disconnectHeader = new Header(Map.of(
                    "END", Integer.toString(1)
            ));

            final Body disconnectBody = new Body("DISCONNECT".getBytes(StandardCharsets.UTF_8));

            final Message disconnectMessage = new Message(disconnectHeader, disconnectBody);

            ctx.router.performServerRequest(
                    disconnectMessage.constructMessage().getBytes(StandardCharsets.UTF_8),
                    serverIPAddress,
                    null,
                    worldPosition,
                    Queries.TCP_DISCONNECT
            );

            sendBrowserResponseToClient(reconstructedData, new String(urlData, StandardCharsets.UTF_8));
        }
    }

    private void handleEstablishedAckSegment(final EstablishedContext ctx) {
        final int ackNumber = ctx.header.getAckNumber();
        System.out.println("   • Received ACK=" + ackNumber + " from server at " + serverIPAddress);

        if (ackNumber == lastAckNumber) {
            duplicateAckCount++;
            System.out.println("🔁 [Browser] Duplicate ACK #" + duplicateAckCount + " detected for " + ackNumber);

            if (duplicateAckCount >= 1) {
                System.out.println("⚡ [Browser] duplicate ACKs detected → Performing FAST RETRANSMIT!");
                fastRetransmit(ctx.router, ackNumber);
                duplicateAckCount = 0;
            }
        } else {
            duplicateAckCount = 0;
            lastAckNumber = ackNumber;
        }

        if (slidingWindow == null) return;

        slidingWindow.acknowledge(ackNumber);

        final int unackedCount = slidingWindow.getUnacknowledgedPackets().size();
        System.out.println("   • Updated sliding window with ACK=" + ackNumber);
        System.out.println("   • Last Acked SEQ in window: " + slidingWindow.getLastAckedSeq());
        System.out.println("   • Unacknowledged packets in window: " + unackedCount);
        System.out.println("   • Unacknowledged packets details:");
        slidingWindow.getUnacknowledgedPackets().forEach(unacknowledgedPacket -> {
            System.out.println("      - SEQ=" + new Header(unacknowledgedPacket.getData()).getSeqNumber());
            System.out.println("      - Data: " + new Message(unacknowledgedPacket.getData()).getBody().extractBody());
        });

        if (unackedCount < slidingWindow.getWindowSize()) {
            System.out.println("🪶 [Browser] Window has space (" + unackedCount + "/3). Sending next packet(s)...");
            sendPacketWithSlidingWindow(ctx.router);
        } else {
            System.out.println("🚫 [Browser] Window full. Waiting for more ACKs.");
        }

        if (unackedCount == 0) {
            System.out.println("✅ [Browser] All data packets acknowledged by server.");
        }
    }

    private boolean isDeliveredDataContiguous(Map<Integer, Body> deliveredMap) {
        return NetworkUtils.isDataContiguous(deliveredMap);
    }

    private void fastRetransmit(final WifiRouterBlockEntity responseRouter, final int ackNumber) {
        if (urlData == null || urlData.length == 0) {
            System.out.println("⚠️ [Browser] No URL data to reconstruct missing packet.");
            return;
        }

        int baseSeq = connectionState.getClientSeq();
        int offset = ackNumber - baseSeq;
        int chunkSize = SlidingWindow.MAX_DATA_SIZE;

        if (offset < 0 || offset >= urlData.length) {
            System.out.println("⚠️ [Browser] Invalid ACK offset for retransmission: " + offset);
            return;
        }

        int end = Math.min(urlData.length, offset + chunkSize);
        byte[] chunk = new byte[end - offset];
        System.arraycopy(urlData, offset, chunk, 0, end - offset);

        boolean isLastChunk = (end == urlData.length);
        byte[] packetBytes = NetworkUtils.createTcpPacket(
                ackNumber,
                connectionState.getServerSeq(),
                isLastChunk,
                chunk
        );


        System.out.println("⚡ [Browser] FAST RETRANSMIT triggered for SEQ=" + ackNumber +
                " | Chunk='" + new String(chunk, StandardCharsets.UTF_8) + "'");

        responseRouter.performServerRequest(
                packetBytes,
                serverIPAddress,
                null,
                worldPosition,
                Queries.TCP_ESTABLISHED
        );
    }

    

    private void retransmitTimedOutPackets(final WifiRouterBlockEntity responseRouter, final List<DataPacket> timedOutPackets) {
        for (final DataPacket packet : timedOutPackets) {
            responseRouter.performServerRequest(
                    packet.getData(),
                    serverIPAddress,
                    null,
                    worldPosition,
                    packet.getQueryType()
            );

            Header header = new Header(packet.getData());
            int seqNum = header.getSeqNumber();
            System.out.println("🚀 [Browser] Retransmitted timed-out data packet with SEQ=" + seqNum);
        }
    }

    private void sendPacketWithSlidingWindow(final WifiRouterBlockEntity responseRouter) {
        if (slidingWindow == null) {
            System.out.println("⚠️ [Browser] Sliding window not initialized. Handshake may not be complete.");
            return;
        }

        final List<DataPacket> packetsToSend = slidingWindow.getPacketsToSend();

        for (DataPacket packet : packetsToSend) {
            responseRouter.performServerRequest(
                    packet.getData(),
                    serverIPAddress,
                    null,
                    worldPosition,
                    packet.getQueryType()
            );

            Header header = new Header(packet.getData());
            int seqNum = header.getSeqNumber();
            System.out.println("🚀 [Browser] Sent data packet with SEQ=" + seqNum);
        }

    }

    private void sendUrlData(final WifiRouterBlockEntity responseRouter, final DataPacket serverResponsePacket) {
        if (urlData == null || urlData.length == 0) {
            System.out.println("⚠️ [Browser] No URL data to send.");
            return;
        }

        if (slidingWindow == null) {
            System.out.println("⚠️ [Browser] Sliding window not initialized. Handshake may not be complete.");
            return;
        }

        final int chunkSize = SlidingWindow.MAX_DATA_SIZE;
        final int baseSeq = connectionState.getClientSeq();
        final int ackBase = connectionState.getServerSeq();

        System.out.println("🌐 [Browser] Sending URL data to server at " + serverIPAddress);
        System.out.println("   • Base Seq: " + baseSeq + ", Ack Base: " + ackBase);


        final List<byte[]> chunks = NetworkUtils.createDataChunks(urlData, chunkSize);
        for (int i = 0; i < chunks.size(); i++) {
            final byte[] chunk = chunks.get(i);
            final int seq = baseSeq + (i * chunkSize);
            final boolean isLastChunk = (i == chunks.size() - 1);

            final byte[] packetBytes = NetworkUtils.createTcpPacket(
                    seq,
                    ackBase,
                    isLastChunk,
                    chunk
            );

            // Construct DataPacket with all routing and addressing info
            final DataPacket packet = serverResponsePacket
                    .updateQueryType(Queries.TCP_ESTABLISHED)
                    .updateData(packetBytes)
                    .updateErrorCode(ErrorCodes.NOERROR)
                    .invertRouterPath()
                    .swapSenderAndReceiver();

            final List<BlockPos> cablePath = NetworkUtils.getCablePathToNextRouter(level, packet.getRouterPath(), packet.getRouterPath().get(0));

            final DataPacket finalPacket = packet.updateCablePath(cablePath);

            slidingWindow.queueData(finalPacket);
        }

        sendPacketWithSlidingWindow(responseRouter);
    }


    public void setUrlData(final byte[] urlData) {
        this.urlData = urlData;
    }

    private void sendBrowserResponseToClient(final String message, final String fileName) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ExampleMod.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> serverLevel.getChunkAt(worldPosition)),
                new BrowserResponsePacket(worldPosition, message, fileName)
        );
    }


    private BlockPos convertBytesToBlockPos(final byte[] bytes) {
        final String posString = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("✅ [Browser] Received DNS Response: " + posString);

        final String[] coords = posString.split(":");
        if (coords.length != 3) {
            throw new IllegalArgumentException("Invalid BlockPos byte array");
        }
        try {
            final int x = Integer.parseInt(coords[0]);
            final int y = Integer.parseInt(coords[1]);
            final int z = Integer.parseInt(coords[2]);
            return new BlockPos(x, y, z);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinates in BlockPos byte array", e);
        }
    }
}
