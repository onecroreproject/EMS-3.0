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
                        .connectTimeout(
                                Duration.ofSeconds(15)
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

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        metadata.getDownloadUrl()
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
