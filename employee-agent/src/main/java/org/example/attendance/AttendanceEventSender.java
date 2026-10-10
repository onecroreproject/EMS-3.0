package org.example.attendance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AttendanceEventSender {

    private final HttpClient httpClient;
    private final String serverUrl;

    public AttendanceEventSender(String serverUrl) {

        this.httpClient = HttpClient.newHttpClient();
        this.serverUrl = serverUrl;
    }

    public AttendanceSendResult sendEvent(
            AttendanceEvent event
    ) {

        try {
            if (org.example.config.AgentTokenHolder.getToken() == null || org.example.config.AgentTokenHolder.getToken().isBlank()) {
                if (org.example.config.AgentTokenHolder.getRefreshToken() != null && !org.example.config.AgentTokenHolder.getRefreshToken().isBlank()) {
                    org.example.config.TokenRefreshService.refreshToken(serverUrl);
                }
            }

            String url =
                    serverUrl
                            + "/api/agent/attendance/event"
                            + "?eventId=" + event.getEventId()
                            + "&employeeCode=" + event.getEmployeeCode()
                            + "&deviceId=" + event.getDeviceId()
                            + "&eventType=" + event.getEventType()
                            + "&timestamp="
                            + event.getTimestamp()
                            .atZone(
                                    java.time.ZoneId.systemDefault()
                            )
                            .toInstant();

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                            .timeout(java.time.Duration.ofSeconds(10))
                            .POST(
                                    HttpRequest.BodyPublishers.noBody()
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            int statusCode =
                    response.statusCode();

            if (statusCode == 302 || statusCode == 401 || statusCode == 403 || statusCode == 500) {
                if (org.example.config.AgentTokenHolder.getRefreshToken() != null && !org.example.config.AgentTokenHolder.getRefreshToken().isBlank()) {
                    boolean refreshed = org.example.config.TokenRefreshService.refreshToken(serverUrl);
                    if (refreshed) {
                        HttpRequest retryRequest = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                                .timeout(java.time.Duration.ofSeconds(10))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();
                        response = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());
                        statusCode = response.statusCode();
                    }
                }
            }

            System.out.println(
                    "Attendance Event Response:"
            );

            System.out.println(
                    "HTTP Status: "
                            + statusCode
            );

            System.out.println(
                    "Response: "
                            + response.body()
            );

            /*
             * =====================================================
             * 2xx = SUCCESS
             *
             * Server accepted and processed the event.
             * =====================================================
             */
            if (statusCode >= 200
                    && statusCode < 300) {

                return AttendanceSendResult.SUCCESS;
            }

            /*
             * =====================================================
             * 4xx = REJECTED
             *
             * The request reached the server, but the server
             * rejected it.
             *
             * Do NOT retry these events automatically.
             * =====================================================
             */
            if (statusCode >= 400
                    && statusCode < 500) {

                System.out.println(
                        "Attendance event rejected by server. "
                                + "Event will NOT be retried. HTTP "
                                + statusCode
                );

                return AttendanceSendResult.REJECTED;
            }

            /*
             * =====================================================
             * 5xx = RETRY
             *
             * Server-side failure.
             * Keep the event in the persistent queue.
             * =====================================================
             */
            if (statusCode >= 500) {

                System.out.println(
                        "Server error while sending attendance "
                                + "event. Event will be retried."
                );

                return AttendanceSendResult.RETRY;
            }

            /*
             * =====================================================
             * Unexpected HTTP status.
             * Treat it as temporary failure.
             * =====================================================
             */
            System.out.println(
                    "Unexpected HTTP status: "
                            + statusCode
                            + ". Event will be retried."
            );

            return AttendanceSendResult.RETRY;

        } catch (Exception e) {

            /*
             * =====================================================
             * Network failure / server unavailable.
             *
             * Keep the event in the persistent queue.
             * =====================================================
             */
            System.out.println(
                    "Attendance event failed. "
                            + "Event will be queued for retry."
            );

            System.out.println(
                    "Reason: "
                            + e.getClass().getSimpleName()
                            + " - "
                            + e.getMessage()
            );

            return AttendanceSendResult.RETRY;
        }
    }
}