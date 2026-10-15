package org.example.ota;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

public class OtaUpdateService {

    private static final Path OTA_DIRECTORY =
            Path.of(
                    "C:\\ProgramData\\EmployeeAgent\\updates"
            );

    private final HttpClient httpClient;

    public OtaUpdateService() {

        this.httpClient =
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(
                                Duration.ofSeconds(30)
                        )
                        .build();
    }

    public Path downloadAndVerify(
            UpdateMetadata metadata)
            throws Exception {

        Files.createDirectories(
                OTA_DIRECTORY
        );

        String fileName =
                "EmployeeMonitoringAgent-"
                        + metadata.getVersion()
                        + ".msi";

        Path downloadedFile =
                OTA_DIRECTORY.resolve(
                        fileName
                );

        System.out.println(
                "OTA: DOWNLOADING"
        );

        String targetUrl = metadata.getDownloadUrl();
        if (targetUrl != null && targetUrl.startsWith("http://ems.dlksoftwaresolutions.co.in")) {
            targetUrl = targetUrl.replace("http://ems.dlksoftwaresolutions.co.in", "https://ems.dlksoftwaresolutions.co.in");
        } else if (targetUrl != null && targetUrl.startsWith("http://")) {
            try {
                String configuredServerUrl = new org.example.config.AgentConfig().getServerUrl();
                if (configuredServerUrl != null && configuredServerUrl.startsWith("https://")) {
                    targetUrl = "https://" + targetUrl.substring(7);
                }
            } catch (Exception ignored) {
            }
        }

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        targetUrl
                                )
                        )
                        .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                        .timeout(
                                Duration.ofMinutes(10)
                        )
                        .header(
                                "Accept",
                                "application/octet-stream"
                        )
                        .GET()
                        .build();

        HttpResponse<InputStream> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream()
                );

        // Fallback: Manually follow redirect if 301/302 was received
        int code = response.statusCode();
        if ((code == 301 || code == 302 || code == 307 || code == 308) && response.headers().firstValue("Location").isPresent()) {
            String redirectUrl = response.headers().firstValue("Location").get();
            System.out.println("OTA: Following manual redirect to " + redirectUrl);
            HttpRequest redirectRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(redirectUrl))
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                            .timeout(Duration.ofMinutes(10))
                            .header("Accept", "application/octet-stream")
                            .GET()
                            .build();
            response = httpClient.send(redirectRequest, HttpResponse.BodyHandlers.ofInputStream());
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                    "OTA download failed. HTTP "
                            + response.statusCode()
            );
        }

        Path temporaryFile =
                OTA_DIRECTORY.resolve(
                        fileName + ".download"
                );

        try (
                InputStream inputStream =
                        response.body();

                OutputStream outputStream =
                        Files.newOutputStream(
                                temporaryFile
                        )
        ) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;

            while (
                    (bytesRead =
                            inputStream.read(buffer))
                            != -1
            ) {

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }

        System.out.println(
                "OTA: DOWNLOADED"
        );

        String actualSha256 =
                calculateSha256(
                        temporaryFile
                );

        String expectedSha256 =
                metadata.getSha256()
                        .trim()
                        .toLowerCase();

        if (!actualSha256.equals(expectedSha256)) {

            Files.deleteIfExists(
                    temporaryFile
            );

            throw new SecurityException(
                    "OTA SHA-256 verification failed. "
                            + "Expected="
                            + expectedSha256
                            + ", Actual="
                            + actualSha256
            );
        }

        Files.move(
                temporaryFile,
                downloadedFile,
                StandardCopyOption.REPLACE_EXISTING
        );

        System.out.println(
                "OTA: VERIFYING"
        );

        System.out.println(
                "OTA: Update saved to: "
                        + downloadedFile
        );

        return downloadedFile;
    }

    private String calculateSha256(
            Path file)
            throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        try (
                InputStream inputStream =
                        Files.newInputStream(file)
        ) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;

            while (
                    (bytesRead =
                            inputStream.read(buffer))
                            != -1
            ) {

                digest.update(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }

        return HexFormat.of()
                .formatHex(
                        digest.digest()
                );
    }
}
