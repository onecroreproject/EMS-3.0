package org.example.commucnication;

import org.example.device.DeviceInfoService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DeviceRegistrationService {

    private final HttpClient httpClient;
    private final String serverUrl;
    private final String deviceId;
    private final DeviceInfoService deviceInfoService;

    public DeviceRegistrationService(
            String serverUrl,
            String deviceId) {

        this.httpClient = HttpClient.newHttpClient();
        this.serverUrl = serverUrl;
        this.deviceId = deviceId;
        this.deviceInfoService =
                new DeviceInfoService();
    }

    public boolean registerDevice(
            String employeeId,
            String employeeCode,
            String token) {

        try {

            String url =
                    serverUrl + "/api/agent/register";

            // Automatically detect device information
            String hostname =
                    deviceInfoService.getHostname();

            String operatingSystem =
                    deviceInfoService.getOperatingSystem();

            String ipAddress =
                    org.example.device.DeviceLocationResolver.getIpAddress();

            String location =
                    org.example.device.DeviceLocationResolver.getLocation();

            String agentVersion =
                    org.example.AgentVersion.VERSION;

            String requestBody = """
                    {
                        "deviceId": "%s",
                        "employeeId": "%s",
                        "employeeCode": "%s",
                        "hostname": "%s",
                        "operatingSystem": "%s",
                        "agentVersion": "%s",
                        "ipAddress": "%s",
                        "location": "%s"
                    }
                    """.formatted(
                    deviceId,
                    employeeId,
                    employeeCode,
                    hostname,
                    operatingSystem,
                    agentVersion,
                    ipAddress,
                    location
            );

            String authToken = (token != null && !token.isBlank()) ? token : org.example.config.AgentTokenHolder.getToken();

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + authToken
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(requestBody)
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Device Registration Response:"
            );

            System.out.println(
                    "HTTP Status: "
                            + response.statusCode()
            );

            System.out.println(
                    "Response: "
                            + response.body()
            );

            return response.statusCode() == 200;

        } catch (Exception e) {

            System.out.println(
                    "Device registration failed: "
                            + e.getMessage()
            );

            return false;
        }
    }
}