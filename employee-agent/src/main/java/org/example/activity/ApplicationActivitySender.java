package org.example.activity;

import org.example.activity.request.ApplicationActivityRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApplicationActivitySender {

    private final HttpClient httpClient;
    private final String serverUrl;

    public ApplicationActivitySender(String serverUrl) {

        this.httpClient = HttpClient.newHttpClient();
        this.serverUrl = serverUrl;
    }

    public boolean sendActivity(
            ApplicationActivityRequest activity
    ) {

        try {
            if (org.example.config.AgentTokenHolder.getToken() == null || org.example.config.AgentTokenHolder.getToken().isBlank()) {
                if (org.example.config.AgentTokenHolder.getRefreshToken() != null && !org.example.config.AgentTokenHolder.getRefreshToken().isBlank()) {
                    org.example.config.TokenRefreshService.refreshToken(serverUrl);
                }
            }

            String json = buildJson(activity);

            String url =
                    serverUrl + "/api/agent/activity";

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(json)
                            )
                            .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 302 || response.statusCode() == 401 || response.statusCode() == 403 || response.statusCode() == 500) {
                if (org.example.config.AgentTokenHolder.getRefreshToken() != null && !org.example.config.AgentTokenHolder.getRefreshToken().isBlank()) {
                    boolean refreshed = org.example.config.TokenRefreshService.refreshToken(serverUrl);
                    if (refreshed) {
                        HttpRequest retryRequest = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build();
                        response = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());
                    }
                }
            }

            System.out.println(
                    "Application Activity Response:"
            );

            System.out.println(
                    "HTTP Status: "
                            + response.statusCode()
            );

            System.out.println(
                    "Response: "
                            + response.body()
            );

            return response.statusCode() >= 200
                    && response.statusCode() < 300;

        } catch (Exception e) {

            System.out.println(
                    "Application activity failed: "
                            + e.getMessage()
            );

            return false;
        }
    }

    private String buildJson(
            ApplicationActivityRequest activity
    ) {

        return "{"
                + "\"employeeCode\":\""
                + escape(activity.getEmployeeCode())
                + "\","

                + "\"deviceId\":\""
                + escape(activity.getDeviceId())
                + "\","

                + "\"processName\":\""
                + escape(activity.getProcessName())
                + "\","

                + "\"processId\":"
                + activity.getProcessId()
                + ","

                + "\"windowTitle\":\""
                + escape(activity.getWindowTitle())
                + "\","

                // NEW
                + "\"url\":\""
                + escape(activity.getUrl())
                + "\","

                // NEW
                + "\"domain\":\""
                + escape(activity.getDomain())
                + "\","

                + "\"startedAt\":\""
                + activity.getStartedAt()
                + "\","

                + "\"endedAt\":\""
                + activity.getEndedAt()
                + "\","

                + "\"durationSeconds\":"
                + activity.getDurationSeconds()

                + "}";
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
