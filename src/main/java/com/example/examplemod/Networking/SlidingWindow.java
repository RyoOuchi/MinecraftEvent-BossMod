package com.example.examplemod.Networking;

import com.example.examplemod.Networking.Data.Body;
import com.example.examplemod.Networking.Data.Message;
import com.example.examplemod.Networking.DataPacket;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class SlidingWindow {

    public static final int DEFAULT_WINDOW_SIZE = 3;
    public static final int DEFAULT_TIMEOUT_MILLIS = 3000;
    public static final int MAX_DATA_SIZE = 8;

    private final int windowSize;
    private final Queue<DataPacket> sendQueue = new LinkedList<>();
    private final Map<Integer, TimedPacket> inFlight = new HashMap<>();
    private int nextSeqNumber;
    private int lastAckedSeq;

    private static final class TimedPacket {
        final DataPacket packet;
        long lastSentTime;
        TimedPacket(final DataPacket packet) {
            this.packet = packet;
            this.lastSentTime = System.currentTimeMillis();
        }

        void refreshTimestamp() {
            this.lastSentTime = System.currentTimeMillis();
        }
    }

    public SlidingWindow(int initialSeq, int windowSize) {
        this.nextSeqNumber = initialSeq;
        this.windowSize = windowSize;
        this.lastAckedSeq = initialSeq;
    }

    public void queueData(DataPacket packet) {
        sendQueue.offer(packet);
    }

    // move packets from queue to in-flight window
    public List<DataPacket> getPacketsToSend() {
        List<DataPacket> ready = new ArrayList<>();
        while (!sendQueue.isEmpty() && inFlight.size() < windowSize) {
            DataPacket packet = sendQueue.poll();
            Message message = new Message(packet.getData());
            Body body = message.getBody();
            int payloadLen = body.extractBody().getBytes(StandardCharsets.UTF_8).length;

            inFlight.put(nextSeqNumber, new TimedPacket(packet));
            ready.add(packet);

            nextSeqNumber += payloadLen;
        }
        return ready;
    }

    public void acknowledge(int ackNumber) {
        Iterator<Map.Entry<Integer, TimedPacket>> it = inFlight.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, TimedPacket> entry = it.next();
            int seq = entry.getKey();
            Message message = new Message(entry.getValue().packet.getData());
            int payloadLength = message.getBody().extractBody().getBytes(StandardCharsets.UTF_8).length;

            System.out.println("🔎 Checking SEQ=" + seq + " len=" + payloadLength + " ack=" + ackNumber);

            if (seq + payloadLength <= ackNumber) {
                it.remove();
                lastAckedSeq = ackNumber;
            }
        }
    }

    public List<DataPacket> getUnacknowledgedPackets() {
        List<DataPacket> packets = new ArrayList<>();
        for (TimedPacket t : inFlight.values()) {
            packets.add(t.packet);
        }
        return packets;
    }

    // return packets that have timed out (not ACKed for too long)
    // reset their timestamps for retransmission
    public List<DataPacket> getTimedOutPackets() {
        long now = System.currentTimeMillis();
        List<DataPacket> timedOut = new ArrayList<>();
        for (TimedPacket timed : inFlight.values()) {
            if (now - timed.lastSentTime > DEFAULT_TIMEOUT_MILLIS) {
                timedOut.add(timed.packet);
                timed.refreshTimestamp();
            }
        }
        return timedOut;
    }

    public int getLastAckedSeq() {
        return lastAckedSeq;
    }

    public int getWindowSize() {
        return windowSize;
    }
}