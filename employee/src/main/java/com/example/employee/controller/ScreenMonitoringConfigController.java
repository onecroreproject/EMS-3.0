package com.example.employee.controller;

import com.example.employee.model.ScreenMonitoringConfig;
import com.example.employee.service.ScreenMonitoringConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/screen-monitoring")
public class ScreenMonitoringConfigController {

    private final ScreenMonitoringConfigService configService;

    public ScreenMonitoringConfigController(ScreenMonitoringConfigService configService) {
        this.configService = configService;
    }

    /**
     * Get current screen monitoring & attendance configuration.
     */
    @GetMapping("/config")
    public ResponseEntity<ScreenMonitoringConfig> getConfig() {
        return ResponseEntity.ok(configService.getConfig());
    }

    /**
     * Update configuration with validation and audit trail.
     */
    @PutMapping("/config")
    public ResponseEntity<?> updateConfig(
            @RequestBody ScreenMonitoringConfig incoming,
            Authentication authentication) {
        try {
            String adminUser = authentication != null ? authentication.getName() : "Admin";
            ScreenMonitoringConfig saved = configService.saveConfig(incoming, adminUser);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save configuration: " + e.getMessage()));
        }
    }

    /**
     * Get default initial configuration values.
     */
    @GetMapping("/config/default")
    public ResponseEntity<ScreenMonitoringConfig> getDefaultConfig() {
        return ResponseEntity.ok(configService.getDefaultConfig());
    }

    /**
     * Get storage usage metrics.
     */
    @GetMapping("/storage")
    public ResponseEntity<?> getStorageMetrics() {
        return ResponseEntity.ok(configService.getStorageMetrics());
    }

    /**
     * Preview cleanup without deleting any records.
     */
    @PostMapping("/cleanup/preview")
    public ResponseEntity<?> previewCleanup() {
        return ResponseEntity.ok(configService.previewCleanup());
    }

    /**
     * Execute authorized cleanup.
     */
    @PostMapping("/cleanup/execute")
    public ResponseEntity<?> executeCleanup(Authentication authentication) {
        String adminUser = authentication != null ? authentication.getName() : "Admin";
        return ResponseEntity.ok(configService.executeCleanup(adminUser));
    }

    /**
     * Get configuration audit change history.
     */
    @GetMapping("/config/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(configService.getHistory());
    }
}
