package com.example.employee.service;

import com.example.employee.model.ScreenMonitoringConfig;
import com.example.employee.model.ScreenMonitoringConfigHistory;
import com.example.employee.model.Screenshot;
import com.example.employee.repository.ScreenMonitoringConfigHistoryRepository;
import com.example.employee.repository.ScreenMonitoringConfigRepository;
import com.example.employee.repository.ScreenshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ScreenMonitoringConfigService {

    private final ScreenMonitoringConfigRepository configRepository;
    private final ScreenMonitoringConfigHistoryRepository historyRepository;
    private final ScreenshotRepository screenshotRepository;

    public ScreenMonitoringConfigService(
            ScreenMonitoringConfigRepository configRepository,
            ScreenMonitoringConfigHistoryRepository historyRepository,
            ScreenshotRepository screenshotRepository) {
        this.configRepository = configRepository;
        this.historyRepository = historyRepository;
        this.screenshotRepository = screenshotRepository;
    }

    /**
     * Retrieve the current configuration, initializing defaults if none exists.
     */
    public synchronized ScreenMonitoringConfig getConfig() {
        Optional<ScreenMonitoringConfig> opt = configRepository.findFirstByOrderByIdAsc();
        if (opt.isPresent()) {
            return opt.get();
        }
        // Initialize default configuration
        ScreenMonitoringConfig defaults = new ScreenMonitoringConfig();
        defaults.setConfigurationVersion(1);
        defaults.setVersionString("v2.1.0");
        defaults.setStatus("ACTIVE");
        defaults.setUpdatedAt(LocalDateTime.now());
        defaults.setUpdatedBy("Admin");
        return configRepository.save(defaults);
    }

    /**
     * Save updated configuration with validation and audit logging.
     */
    public synchronized ScreenMonitoringConfig saveConfig(ScreenMonitoringConfig incoming, String adminUser) {
        validateConfig(incoming);

        ScreenMonitoringConfig current = getConfig();

        // Increment version
        int newVersion = current.getConfigurationVersion() + 1;
        incoming.setId(current.getId());
        incoming.setConfigurationVersion(newVersion);
        incoming.setVersionString("v2." + newVersion + ".0");
        incoming.setStatus("ACTIVE");
        incoming.setUpdatedAt(LocalDateTime.now());
        incoming.setUpdatedBy(adminUser != null && !adminUser.isBlank() ? adminUser : "Admin");
        incoming.setCreatedAt(current.getCreatedAt() != null ? current.getCreatedAt() : LocalDateTime.now());

        ScreenMonitoringConfig saved = configRepository.save(incoming);

        // Record history snapshot
        try {
            ScreenMonitoringConfigHistory history = new ScreenMonitoringConfigHistory(
                    saved.getConfigurationVersion(),
                    saved.getVersionString(),
                    saved.getUpdatedBy(),
                    LocalDateTime.now(),
                    "Updated Screen Monitoring & Attendance settings to " + saved.getVersionString(),
                    saved
            );
            historyRepository.save(history);
        } catch (Exception e) {
            System.err.println("Failed to write audit history: " + e.getMessage());
        }

        return saved;
    }

    /**
     * Reset configuration to initial defaults.
     */
    public ScreenMonitoringConfig getDefaultConfig() {
        ScreenMonitoringConfig def = new ScreenMonitoringConfig();
        ScreenMonitoringConfig current = getConfig();
        def.setId(current.getId());
        def.setConfigurationVersion(current.getConfigurationVersion());
        def.setVersionString(current.getVersionString());
        return def;
    }

    /**
     * Preview cleanup without deleting any records.
     */
    public Map<String, Object> previewCleanup() {
        ScreenMonitoringConfig config = getConfig();
        int retentionDays = Math.max(1, config.getRetentionPeriod());
        long cutoffMillis = System.currentTimeMillis() - (retentionDays * 86400000L);
        Date cutoffDate = new Date(cutoffMillis);

        List<Screenshot> all = screenshotRepository.findAll();
        long eligibleCount = 0;
        long totalBytes = 0;
        Set<String> affectedEmployees = new HashSet<>();

        for (Screenshot s : all) {
            if (s.getTimestamp() != null && s.getTimestamp().before(cutoffDate)) {
                eligibleCount++;
                if (s.getImageData() != null) {
                    totalBytes += s.getImageData().length;
                }
                if (s.getEmployeeId() != null) {
                    affectedEmployees.add(s.getEmployeeId());
                }
            }
        }

        double estimatedMb = Math.round((totalBytes / (1024.0 * 1024.0)) * 10.0) / 10.0;

        Map<String, Object> preview = new HashMap<>();
        preview.put("eligibleCount", eligibleCount);
        preview.put("estimatedStorageMb", estimatedMb);
        preview.put("cutoffTimestamp", cutoffDate.toString());
        preview.put("retentionDays", retentionDays);
        preview.put("affectedEmployeesCount", affectedEmployees.size());
        return preview;
    }

    /**
     * Execute authorized cleanup of screenshots exceeding the retention period.
     */
    public Map<String, Object> executeCleanup(String adminUser) {
        ScreenMonitoringConfig config = getConfig();
        int retentionDays = Math.max(1, config.getRetentionPeriod());
        long cutoffMillis = System.currentTimeMillis() - (retentionDays * 86400000L);
        Date cutoffDate = new Date(cutoffMillis);

        List<Screenshot> all = screenshotRepository.findAll();
        long deletedCount = 0;
        long bytesFreed = 0;

        for (Screenshot s : all) {
            if (s.getTimestamp() != null && s.getTimestamp().before(cutoffDate)) {
                if (s.getImageData() != null) {
                    bytesFreed += s.getImageData().length;
                }
                screenshotRepository.deleteById(s.getId());
                deletedCount++;
            }
        }

        double freedMb = Math.round((bytesFreed / (1024.0 * 1024.0)) * 10.0) / 10.0;

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("storageFreedMb", freedMb);
        result.put("executedAt", LocalDateTime.now().toString());
        result.put("executedBy", adminUser != null ? adminUser : "Admin");
        return result;
    }

    /**
     * Retrieve storage metrics.
     */
    public Map<String, Object> getStorageMetrics() {
        List<Screenshot> all = screenshotRepository.findAll();
        long totalCount = all.size();
        long totalBytes = 0;
        Set<String> employees = new HashSet<>();

        for (Screenshot s : all) {
            if (s.getImageData() != null) {
                totalBytes += s.getImageData().length;
            }
            if (s.getEmployeeId() != null) {
                employees.add(s.getEmployeeId());
            }
        }

        double totalGb = Math.round((totalBytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0;
        double totalMb = Math.round((totalBytes / (1024.0 * 1024.0)) * 10.0) / 10.0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalScreenshots", totalCount);
        metrics.put("totalStorageGb", totalGb);
        metrics.put("totalStorageMb", totalMb);
        metrics.put("displayStorage", totalGb >= 1.0 ? totalGb + " GB" : totalMb + " MB");
        metrics.put("activeEmployeesCount", employees.size());
        return metrics;
    }

    /**
     * Retrieve audit history.
     */
    public List<ScreenMonitoringConfigHistory> getHistory() {
        return historyRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Strict backend validation rules.
     */
    private void validateConfig(ScreenMonitoringConfig c) {
        if (c.getCaptureInterval() < 1 || c.getCaptureInterval() > 60) {
            throw new IllegalArgumentException("Capture interval must be between 1 and 60 minutes.");
        }
        if (c.getImageQuality() < 10 || c.getImageQuality() > 100) {
            throw new IllegalArgumentException("Image quality must be between 10% and 100%.");
        }
        if (!"JPEG".equalsIgnoreCase(c.getImageFormat()) && !"PNG".equalsIgnoreCase(c.getImageFormat())) {
            throw new IllegalArgumentException("Image format must be JPEG or PNG.");
        }
        if (c.getRetentionPeriod() < 1) {
            throw new IllegalArgumentException("Retention period must be at least 1 day.");
        }
        if (c.getCleanupBatchSize() < 10 || c.getCleanupBatchSize() > 10000) {
            throw new IllegalArgumentException("Cleanup batch size must be between 10 and 10,000.");
        }
        if (c.getStorageUsageWarning() < 10 || c.getStorageUsageWarning() > 100) {
            throw new IllegalArgumentException("Storage warning threshold must be between 10% and 100%.");
        }
        if (c.getMaxUploadSizeMb() < 1 || c.getMaxUploadSizeMb() > 50) {
            throw new IllegalArgumentException("Maximum upload size must be between 1 MB and 50 MB.");
        }
        if (c.getIdleDetectionThreshold() < 1 || c.getIdleDetectionThreshold() > 60) {
            throw new IllegalArgumentException("Idle detection threshold must be between 1 and 60 minutes.");
        }
        if (c.getDailyRequiredWorkingHours() < 1 || c.getDailyRequiredWorkingHours() > 24) {
            throw new IllegalArgumentException("Daily required working hours must be between 1 and 24 hours.");
        }
        if (c.getMarkOfflineAfterContinuousIdle() < 5 || c.getMarkOfflineAfterContinuousIdle() > 240) {
            throw new IllegalArgumentException("Mark offline threshold must be between 5 and 240 minutes.");
        }
    }
}
