package org.example.heartbeat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HeartbeatService {

    private final HttpClient httpClient;
    private final String serverUrl;
    private final String deviceId;

    public HeartbeatService(String serverUrl, String deviceId) {
        this.httpClient = HttpClient.newHttpClient();
        this.serverUrl = serverUrl;
        this.deviceId = deviceId;
    }

    public String sendHeartbeat() {

        try {
            if (org.example.config.AgentTokenHolder.getToken() == null || org.example.config.AgentTokenHolder.getToken().isBlank()) {
                if (org.example.config.AgentTokenHolder.getRefreshToken() != null && !org.example.config.AgentTokenHolder.getRefreshToken().isBlank()) {
                    org.example.config.TokenRefreshService.refreshToken(serverUrl);
                }
            }

            String url = serverUrl
                    + "/api/agent/heartbeat?deviceId="
                    + deviceId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() == 302 || response.statusCode() == 401 || response.statusCode() == 403 || response.statusCode() == 500) {
                // The server might return 302 or 500 for malformed JWT. Let's try refreshing if the token is invalid
                if (org.example.config.AgentTokenHolder.getRefreshToken() != null && !org.example.config.AgentTokenHolder.getRefreshToken().isBlank()) {
                    boolean refreshed = org.example.config.TokenRefreshService.refreshToken(serverUrl);
                    if (refreshed) {
                        // Retry heartbeat once
                        HttpRequest retryRequest = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();
                        response = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());
                        
                        if (response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204) {
                            return response.body();
                        }
                    }
                }
                return null;
            }

            return response.body();

        } catch (Exception e) {

            System.out.println("WARN  Heartbeat failed: " + e.getMessage());
            return null;
        }
    }

}
