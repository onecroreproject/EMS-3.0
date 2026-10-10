package org.example.ota;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.AgentVersion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateChecker {

    private static final String UPDATE_ENDPOINT =
            "/api/v1/agent-updates/latest";

    private final String serverUrl;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public UpdateChecker(String serverUrl) {

        if (serverUrl == null
                || serverUrl.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Server URL cannot be empty"
            );
        }

        this.serverUrl =
                serverUrl
                        .trim()
                        .replaceAll("/+$", "");

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(10)
                        )
                        .build();

        this.objectMapper =
                new ObjectMapper();
    }

    public UpdateMetadata checkForUpdate()
            throws Exception {

        String url =
                serverUrl
                        + UPDATE_ENDPOINT
                        + "?currentVersion="
                        + AgentVersion.VERSION
                        + "&platform=windows";

        System.out.println(
                "OTA: CHECKING"
        );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                        .timeout(
                                Duration.ofSeconds(15)
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() == 204) {

            System.out.println(
                    "OTA: No update available."
            );

            return null;
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                    "OTA update check failed. HTTP "
                            + response.statusCode()
            );
        }

        UpdateMetadata metadata =
                objectMapper.readValue(
                        response.body(),
                        UpdateMetadata.class
                );

        validateMetadata(metadata);

        int comparison =
                VersionComparator.compare(
                        AgentVersion.VERSION,
                        metadata.getVersion()
                );

        if (comparison >= 0) {

            System.out.println(
                    "OTA: Agent is already up to date. "
                            + "Current="
                            + AgentVersion.VERSION
                            + ", Server="
                            + metadata.getVersion()
            );

            return null;
        }

        System.out.println(
                "OTA: UPDATE_AVAILABLE "
                        + metadata.getVersion()
        );

        return metadata;
    }

    private void validateMetadata(
            UpdateMetadata metadata) {

        if (metadata == null) {

            throw new IllegalStateException(
                    "OTA server returned empty metadata"
            );
        }

        if (metadata.getVersion() == null
                || metadata.getVersion().isBlank()) {

            throw new IllegalStateException(
                    "OTA version is missing"
            );
        }

        if (metadata.getDownloadUrl() == null
                || metadata.getDownloadUrl().isBlank()) {

            throw new IllegalStateException(
                    "OTA download URL is missing"
            );
        }

        if (metadata.getSha256() == null
                || metadata.getSha256().isBlank()) {

            throw new IllegalStateException(
                    "OTA SHA-256 is missing"
            );
        }
    }
}
