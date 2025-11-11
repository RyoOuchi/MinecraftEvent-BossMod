package com.example.examplemod.DoNotTouch.Networking;

import net.minecraft.client.Minecraft;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketManager {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private WebSocketClient client;

    public void connect(String serverUri) {
        EXECUTOR.submit(() -> {
            try {
                client = new WebSocketClient(new URI(serverUri)) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        System.out.println("✅ WebSocket connected: " + serverUri);
                    }

                    @Override
                    public void onMessage(String message) {
                        System.out.println("📩 Received: " + message);

                        // Safely interact with Minecraft on main thread
                        Minecraft.getInstance().execute(() -> {

                        });
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        System.out.println("❌ WebSocket closed: " + reason);
                    }

                    @Override
                    public void onError(Exception ex) {
                        ex.printStackTrace();
                    }
                };

                client.connectBlocking();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void sendMessage(String msg) {
        if (client != null && client.isOpen()) {
            client.send(msg);
        }
    }

    public void disconnect() {
        if (client != null) {
            client.close();
        }
    }
}
