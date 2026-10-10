package com.example.employee.controller;

import com.example.employee.dto.AgentUpdatePublishRequest;
import com.example.employee.dto.AgentUpdateResponse;
import com.example.employee.model.AgentUpdate;
import com.example.employee.service.AgentUpdateFileService;
import com.example.employee.service.AgentUpdateService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/agent-updates")
public class AgentUpdateController {

    private final AgentUpdateService agentUpdateService;

    private final AgentUpdateFileService agentUpdateFileService;

    public AgentUpdateController(
            AgentUpdateService agentUpdateService,
            AgentUpdateFileService agentUpdateFileService) {

        this.agentUpdateService = agentUpdateService;
        this.agentUpdateFileService =
                agentUpdateFileService;
    }

    /**
     * Agent checks whether a newer version is available.
     *
     * GET:
     * /api/v1/agent-updates/latest
     *
     * Example:
     * ?currentVersion=1.0.0&platform=windows
     */
    @GetMapping("/latest")
    public ResponseEntity<AgentUpdateResponse> getLatestUpdate(
            @RequestParam String currentVersion,
            @RequestParam(defaultValue = "windows")
            String platform) {

        AgentUpdate update =
                agentUpdateService.getLatestUpdate(
                        currentVersion,
                        platform
                );

        /*
         * No newer version available.
         */
        if (update == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        /*
         * Build download URL for the Employee Agent.
         */
        String downloadUrl =
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .path(
                                "/api/v1/agent-updates/download/{version}"
                        )
                        .queryParam(
                                "platform",
                                update.getPlatform()
                        )
                        .buildAndExpand(
                                update.getVersion()
                        )
                        .toUriString();

        AgentUpdateResponse response =
                new AgentUpdateResponse(
                        update.getVersion(),
                        downloadUrl,
                        update.getSha256(),
                        update.isMandatory(),
                        update.getReleaseNotes()
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Publishes a new Employee Agent MSI.
     *
     * This endpoint receives:
     *
     * version
     * platform
     * mandatory
     * releaseNotes
     * file
     */
    @PostMapping(
            value = "/publish",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AgentUpdateResponse> publishUpdate(

            @Valid
            @ModelAttribute
            AgentUpdatePublishRequest request,

            @RequestPart("file")
            MultipartFile file) throws Exception {

        /*
         * Prevent publishing the same version twice
         * for the same platform.
         */
        AgentUpdate existing =
                agentUpdateService.findByVersion(
                        request.getVersion(),
                        request.getPlatform()
                );

        if (existing != null) {

            return ResponseEntity
                    .status(409)
                    .build();
        }

        /*
         * Store MSI and calculate SHA-256.
         */
        AgentUpdateFileService.StoredUpdateFile storedFile =
                agentUpdateFileService.storeFile(
                        file,
                        request.getVersion()
                );

        try {

            /*
             * Deactivate previous active release.
             */
            agentUpdateService.deactivateActiveUpdates(
                    request.getPlatform()
            );

            /*
             * Create MongoDB update record.
             */
            AgentUpdate agentUpdate =
                    new AgentUpdate();

            agentUpdate.setVersion(
                    request.getVersion()
            );

            agentUpdate.setPlatform(
                    request.getPlatform()
            );

            agentUpdate.setFileName(
                    storedFile.fileName()
            );

            agentUpdate.setStoragePath(
                    storedFile.path().toString()
            );

            agentUpdate.setSha256(
                    storedFile.sha256()
            );

            agentUpdate.setMandatory(
                    request.isMandatory()
            );

            agentUpdate.setReleaseNotes(
                    request.getReleaseNotes()
            );

            agentUpdate.setActive(true);

            agentUpdate.setCreatedAt(
                    java.time.Instant.now()
            );

            AgentUpdate savedUpdate =
                    agentUpdateService.saveUpdate(
                            agentUpdate
                    );

            /*
             * Build download URL.
             */
            String downloadUrl =
                    ServletUriComponentsBuilder
                            .fromCurrentContextPath()
                            .path(
                                    "/api/v1/agent-updates/download/{version}"
                            )
                            .queryParam(
                                    "platform",
                                    savedUpdate.getPlatform()
                            )
                            .buildAndExpand(
                                    savedUpdate.getVersion()
                            )
                            .toUriString();

            AgentUpdateResponse response =
                    new AgentUpdateResponse(
                            savedUpdate.getVersion(),
                            downloadUrl,
                            savedUpdate.getSha256(),
                            savedUpdate.isMandatory(),
                            savedUpdate.getReleaseNotes()
                    );

            return ResponseEntity.ok(response);

        } catch (Exception exception) {

            /*
             * If MongoDB save fails after the MSI was stored,
             * remove the MSI so we don't leave an orphan file.
             */
            try {
                java.nio.file.Files.deleteIfExists(
                        storedFile.path()
                );
            } catch (Exception ignored) {
                // Keep the original exception.
            }

            throw exception;
        }
    }

    /**
     * Downloads a published MSI.
     *
     * GET:
     * /api/v1/agent-updates/download/1.1.0?platform=windows
     */
    @GetMapping("/download/{version}")
    public ResponseEntity<Resource> downloadUpdate(
            @PathVariable String version,
            @RequestParam(defaultValue = "windows")
            String platform) throws Exception {

        AgentUpdate update =
                agentUpdateService.findByVersion(
                        version,
                        platform
                );

        if (update == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        Path filePath =
                Path.of(update.getStoragePath());

        Resource resource =
                agentUpdateFileService.loadFile(
                        filePath
                );

        ContentDisposition contentDisposition =
                ContentDisposition
                        .attachment()
                        .filename(update.getFileName())
                        .build();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .contentLength(
                        resource.contentLength()
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .body(resource);
    }
}