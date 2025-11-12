package com.example.examplemod.DoNotTouch.Networking;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.examplemod.DoNotTouch.ImportantConstants.BACKEND_API_URL;

public class NetworkUtils {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static String getFullApiEndpoint(String endpoint) {
        return "https" + BACKEND_API_URL + endpoint;
    }

    public static String getFullSocketURL() {
        return "wss" + BACKEND_API_URL;
    }

    public static JsonObject createJsonPayloadFromMap(Map<String, String> data) {
        var jsonObjectBuilder = Json.createObjectBuilder();
        for (var entry : data.entrySet()) {
            jsonObjectBuilder.add(entry.getKey(), entry.getValue());
        }
        return jsonObjectBuilder.build();
    }

    public static void performApiPostRequest(String endpoint, Map<String, String> payloadData) {
        EXECUTOR.submit(() -> {
            try {
                String urlStr = getFullApiEndpoint(endpoint);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("ngrok-skip-browser-warning", "true");
                conn.setDoOutput(true);

                JsonObject json = createJsonPayloadFromMap(payloadData);
                String jsonString = json.toString();

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonString.getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();
                System.out.println("API POST Response: " + status + " from " + urlStr);

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void informBackendSummonedBoss(String bossId) {
        //performApiPostRequest(EndPoints.SPAWNED_BOSS.getEndPointPath(), );
    }

    public static void informBackendDefeatedBoss(String bossId) {
        //performApiPostRequest(EndPoints.DEFEATED_BOSS.getEndPointPath(), );
    }
}