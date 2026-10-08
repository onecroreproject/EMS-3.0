package com.example.employee.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AgentUpdateFileService {

    private final Path storageDirectory;

    public AgentUpdateFileService(
            @Value("${agent.update.storage-directory:./agent-updates}")
            String storageDirectory) {

        this.storageDirectory =
                Path.of(storageDirectory)
                        .toAbsolutePath()
                        .normalize();
    }

    /**
     * Stores the uploaded MSI file and calculates its SHA-256 checksum.
     */
    public StoredUpdateFile storeFile(
            MultipartFile file,
            String version) throws IOException {

        validateFile(file);

        Files.createDirectories(storageDirectory);

        String fileName =
                "EmployeeMonitoringAgent-" + version + ".msi";

        Path targetFile =
                storageDirectory
                        .resolve(fileName)
                        .normalize();

        if (!targetFile.startsWith(storageDirectory)) {
            throw new IOException("Invalid update file path");
        }

        if (Files.exists(targetFile)) {
            throw new IOException(
                    "Update file already exists: " + fileName
            );
        }

        Path temporaryFile =
                storageDirectory.resolve(
                        fileName + ".tmp"
                );

        try {

            Files.copy(
                    file.getInputStream(),
                    temporaryFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String sha256 =
                    calculateSha256(temporaryFile);

            Files.move(
                    temporaryFile,
                    targetFile,
                    StandardCopyOption.ATOMIC_MOVE
            );

            return new StoredUpdateFile(
                    fileName,
                    targetFile,
                    sha256
            );

        } catch (Exception exception) {

            Files.deleteIfExists(temporaryFile);
            Files.deleteIfExists(targetFile);

            throw exception;
        }
    }

    /**
     * Loads an existing update file.
     */
    public Resource loadFile(Path filePath) throws IOException {

        Path normalizedPath =
                filePath.toAbsolutePath()
                        .normalize();

        if (!normalizedPath.startsWith(storageDirectory)) {
            throw new IOException(
                    "Access to the requested file is not allowed"
            );
        }

        if (!Files.exists(normalizedPath)) {
            throw new IOException(
                    "Update file not found: " + normalizedPath
            );
        }

        if (!Files.isRegularFile(normalizedPath)) {
            throw new IOException(
                    "Update path is not a regular file"
            );
        }

        Resource resource =
                new UrlResource(
                        normalizedPath.toUri()
                );

        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException(
                    "Update file cannot be read"
            );
        }

        return resource;
    }

    /**
     * Returns the configured storage directory.
     */
    public Path getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Validates the uploaded file.
     */
    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "MSI file is required"
            );
        }

        String originalFilename =
                file.getOriginalFilename();

        if (originalFilename == null
                || originalFilename.isBlank()) {

            throw new IllegalArgumentException(
                    "MSI file name is required"
            );
        }

        String lowerCaseFileName =
                originalFilename.toLowerCase();

        if (!lowerCaseFileName.endsWith(".msi")) {
            throw new IllegalArgumentException(
                    "Only MSI files are allowed"
            );
        }
    }

    /**
     * Calculates SHA-256 for the supplied file.
     */
    private String calculateSha256(Path file)
            throws IOException {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream =
                         Files.newInputStream(file)) {

                byte[] buffer =
                        new byte[8192];

                int bytesRead;

                while ((bytesRead =
                        inputStream.read(buffer)) != -1) {

                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            byte[] hash =
                    digest.digest();

            StringBuilder result =
                    new StringBuilder();

            for (byte value : hash) {

                result.append(
                        String.format(
                                "%02x",
                                value
                        )
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    /**
     * Represents a successfully stored update file.
     */
    public record StoredUpdateFile(
            String fileName,
            Path path,
            String sha256
    ) {
    }
}
