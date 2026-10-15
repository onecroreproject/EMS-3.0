package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "screen_monitoring_config")
public class ScreenMonitoringConfig {

    @Id
    private String id;

    // Metadata & Versioning
    private int configurationVersion = 1;
    private String versionString = "v2.1.0";
    private String status = "ACTIVE";
    private LocalDateTime updatedAt = LocalDateTime.now();
    private String updatedBy = "Admin";
    private LocalDateTime createdAt = LocalDateTime.now();

    // 1. Screenshot Capture Settings
    private boolean captureEnabled = true;
    private int captureInterval = 5;
    private String captureIntervalUnit = "Minutes";
    private int imageQuality = 75;
    private String imageFormat = "JPEG";
    private boolean captureDuringWorking = true;
    private boolean captureDuringIdle = true;
    private boolean captureDuringBreak = true;
    private boolean captureWhenLoggedOut = false;

    // 2. Retention and Automatic Cleanup
    private int retentionPeriod = 30;
    private String retentionPeriodUnit = "Days";
    private boolean automaticCleanup = true;
    private String cleanupScheduleTime = "02:00 AM";
    private String cleanupTimeZone = "Asia/Kolkata (IST)";
    private int cleanupBatchSize = 1000;
    private int storageUsageWarning = 80;

    // 3. Storage and Performance
    private int maxUploadSizeMb = 5;
    private boolean thumbnailGeneration = true;
    private String thumbnailDimensions = "320 x 180";
    private int thumbnailQuality = 70;

    // 4. Offline Upload and Synchronization
    private boolean enableOfflineCapture = true;
    private int localStorageLimit = 2;
    private String localStorageLimitUnit = "GB";
    private int retryInterval = 5;
    private String retryIntervalUnit = "Minutes";
    private int maxRetryAttempts = 10;
    private boolean autoSyncOnStartup = true;

    // 5. Access Control
    private boolean allowScreenshotDownload = true;
    private boolean restrictToAdminUsersOnly = true;
    private boolean auditConfigurationChanges = true;

    // 7. Attendance & Idle Policy
    private int dailyRequiredWorkingHours = 9;
    private String dailyRequiredWorkingHoursUnit = "Hours";
    private int idleDetectionThreshold = 2;
    private String idleDetectionThresholdUnit = "Minutes";
    private int markOfflineAfterContinuousIdle = 30;
    private String markOfflineAfterContinuousIdleUnit = "Minutes";
    private boolean enableAutomaticOfflineStatus = true;
    private boolean applyPolicyPerEmployee = false;

    public ScreenMonitoringConfig() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getConfigurationVersion() { return configurationVersion; }
    public void setConfigurationVersion(int configurationVersion) { this.configurationVersion = configurationVersion; }

    public String getVersionString() { return versionString; }
    public void setVersionString(String versionString) { this.versionString = versionString; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isCaptureEnabled() { return captureEnabled; }
    public void setCaptureEnabled(boolean captureEnabled) { this.captureEnabled = captureEnabled; }

    public int getCaptureInterval() { return captureInterval; }
    public void setCaptureInterval(int captureInterval) { this.captureInterval = captureInterval; }

    public String getCaptureIntervalUnit() { return captureIntervalUnit; }
    public void setCaptureIntervalUnit(String captureIntervalUnit) { this.captureIntervalUnit = captureIntervalUnit; }

    public int getImageQuality() { return imageQuality; }
    public void setImageQuality(int imageQuality) { this.imageQuality = imageQuality; }

    public String getImageFormat() { return imageFormat; }
    public void setImageFormat(String imageFormat) { this.imageFormat = imageFormat; }

    public boolean isCaptureDuringWorking() { return captureDuringWorking; }
    public void setCaptureDuringWorking(boolean captureDuringWorking) { this.captureDuringWorking = captureDuringWorking; }

    public boolean isCaptureDuringIdle() { return captureDuringIdle; }
    public void setCaptureDuringIdle(boolean captureDuringIdle) { this.captureDuringIdle = captureDuringIdle; }

    public boolean isCaptureDuringBreak() { return captureDuringBreak; }
    public void setCaptureDuringBreak(boolean captureDuringBreak) { this.captureDuringBreak = captureDuringBreak; }

    public boolean isCaptureWhenLoggedOut() { return captureWhenLoggedOut; }
    public void setCaptureWhenLoggedOut(boolean captureWhenLoggedOut) { this.captureWhenLoggedOut = captureWhenLoggedOut; }

    public int getRetentionPeriod() { return retentionPeriod; }
    public void setRetentionPeriod(int retentionPeriod) { this.retentionPeriod = retentionPeriod; }

    public String getRetentionPeriodUnit() { return retentionPeriodUnit; }
    public void setRetentionPeriodUnit(String retentionPeriodUnit) { this.retentionPeriodUnit = retentionPeriodUnit; }

    public boolean isAutomaticCleanup() { return automaticCleanup; }
    public void setAutomaticCleanup(boolean automaticCleanup) { this.automaticCleanup = automaticCleanup; }

    public String getCleanupScheduleTime() { return cleanupScheduleTime; }
    public void setCleanupScheduleTime(String cleanupScheduleTime) { this.cleanupScheduleTime = cleanupScheduleTime; }

    public String getCleanupTimeZone() { return cleanupTimeZone; }
    public void setCleanupTimeZone(String cleanupTimeZone) { this.cleanupTimeZone = cleanupTimeZone; }

    public int getCleanupBatchSize() { return cleanupBatchSize; }
    public void setCleanupBatchSize(int cleanupBatchSize) { this.cleanupBatchSize = cleanupBatchSize; }

    public int getStorageUsageWarning() { return storageUsageWarning; }
    public void setStorageUsageWarning(int storageUsageWarning) { this.storageUsageWarning = storageUsageWarning; }

    public int getMaxUploadSizeMb() { return maxUploadSizeMb; }
    public void setMaxUploadSizeMb(int maxUploadSizeMb) { this.maxUploadSizeMb = maxUploadSizeMb; }

    public boolean isThumbnailGeneration() { return thumbnailGeneration; }
    public void setThumbnailGeneration(boolean thumbnailGeneration) { this.thumbnailGeneration = thumbnailGeneration; }

    public String getThumbnailDimensions() { return thumbnailDimensions; }
    public void setThumbnailDimensions(String thumbnailDimensions) { this.thumbnailDimensions = thumbnailDimensions; }

    public int getThumbnailQuality() { return thumbnailQuality; }
    public void setThumbnailQuality(int thumbnailQuality) { this.thumbnailQuality = thumbnailQuality; }

    public boolean isEnableOfflineCapture() { return enableOfflineCapture; }
    public void setEnableOfflineCapture(boolean enableOfflineCapture) { this.enableOfflineCapture = enableOfflineCapture; }

    public int getLocalStorageLimit() { return localStorageLimit; }
    public void setLocalStorageLimit(int localStorageLimit) { this.localStorageLimit = localStorageLimit; }

    public String getLocalStorageLimitUnit() { return localStorageLimitUnit; }
    public void setLocalStorageLimitUnit(String localStorageLimitUnit) { this.localStorageLimitUnit = localStorageLimitUnit; }

    public int getRetryInterval() { return retryInterval; }
    public void setRetryInterval(int retryInterval) { this.retryInterval = retryInterval; }

    public String getRetryIntervalUnit() { return retryIntervalUnit; }
    public void setRetryIntervalUnit(String retryIntervalUnit) { this.retryIntervalUnit = retryIntervalUnit; }

    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }

    public boolean isAutoSyncOnStartup() { return autoSyncOnStartup; }
    public void setAutoSyncOnStartup(boolean autoSyncOnStartup) { this.autoSyncOnStartup = autoSyncOnStartup; }

    public boolean isAllowScreenshotDownload() { return allowScreenshotDownload; }
    public void setAllowScreenshotDownload(boolean allowScreenshotDownload) { this.allowScreenshotDownload = allowScreenshotDownload; }

    public boolean isRestrictToAdminUsersOnly() { return restrictToAdminUsersOnly; }
    public void setRestrictToAdminUsersOnly(boolean restrictToAdminUsersOnly) { this.restrictToAdminUsersOnly = restrictToAdminUsersOnly; }

    public boolean isAuditConfigurationChanges() { return auditConfigurationChanges; }
    public void setAuditConfigurationChanges(boolean auditConfigurationChanges) { this.auditConfigurationChanges = auditConfigurationChanges; }

    public int getDailyRequiredWorkingHours() { return dailyRequiredWorkingHours; }
    public void setDailyRequiredWorkingHours(int dailyRequiredWorkingHours) { this.dailyRequiredWorkingHours = dailyRequiredWorkingHours; }

    public String getDailyRequiredWorkingHoursUnit() { return dailyRequiredWorkingHoursUnit; }
    public void setDailyRequiredWorkingHoursUnit(String dailyRequiredWorkingHoursUnit) { this.dailyRequiredWorkingHoursUnit = dailyRequiredWorkingHoursUnit; }

    public int getIdleDetectionThreshold() { return idleDetectionThreshold; }
    public void setIdleDetectionThreshold(int idleDetectionThreshold) { this.idleDetectionThreshold = idleDetectionThreshold; }

    public String getIdleDetectionThresholdUnit() { return idleDetectionThresholdUnit; }
    public void setIdleDetectionThresholdUnit(String idleDetectionThresholdUnit) { this.idleDetectionThresholdUnit = idleDetectionThresholdUnit; }

    public int getMarkOfflineAfterContinuousIdle() { return markOfflineAfterContinuousIdle; }
    public void setMarkOfflineAfterContinuousIdle(int markOfflineAfterContinuousIdle) { this.markOfflineAfterContinuousIdle = markOfflineAfterContinuousIdle; }

    public String getMarkOfflineAfterContinuousIdleUnit() { return markOfflineAfterContinuousIdleUnit; }
    public void setMarkOfflineAfterContinuousIdleUnit(String markOfflineAfterContinuousIdleUnit) { this.markOfflineAfterContinuousIdleUnit = markOfflineAfterContinuousIdleUnit; }

    public boolean isEnableAutomaticOfflineStatus() { return enableAutomaticOfflineStatus; }
    public void setEnableAutomaticOfflineStatus(boolean enableAutomaticOfflineStatus) { this.enableAutomaticOfflineStatus = enableAutomaticOfflineStatus; }

    public boolean isApplyPolicyPerEmployee() { return applyPolicyPerEmployee; }
    public void setApplyPolicyPerEmployee(boolean applyPolicyPerEmployee) { this.applyPolicyPerEmployee = applyPolicyPerEmployee; }
}
