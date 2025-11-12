package com.example.examplemod.DoNotTouch.Networking;

import com.example.examplemod.DoNotTouch.Packets.SummonEntityPacket;
import com.example.examplemod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketManager {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private WebSocketClient client;
    public static final String WEBSOCKET_URL = NetworkUtils.getFullSocketURL();


    public void connect(String serverUri) {
        EXECUTOR.submit(() -> {
            try {
                client = new WebSocketClient(new URI(serverUri)) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        System.out.println("[WebSocketManager] WebSocket connected: " + serverUri);
                    }

                    @Override
                    public void onMessage(String message) {
                        System.out.println("[WebSocketManager] Received: " + message);

                        Minecraft.getInstance().execute(() -> {
                            Level level = Minecraft.getInstance().level;
                            if (level != null && Minecraft.getInstance().player != null) {
                                ExampleMod.CHANNEL.sendToServer(new SummonEntityPacket(message));
                            }
                        });
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        System.out.println("[WebSocketManager] WebSocket closed: " + reason);
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
