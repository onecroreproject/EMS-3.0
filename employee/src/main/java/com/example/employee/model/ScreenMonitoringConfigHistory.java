package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "screen_monitoring_config_history")
public class ScreenMonitoringConfigHistory {

    @Id
    private String id;

    private int configurationVersion;
    private String versionString;
    private String updatedBy;
    private LocalDateTime timestamp;
    private String actionSummary;
    private ScreenMonitoringConfig configSnapshot;

    public ScreenMonitoringConfigHistory() {}

    public ScreenMonitoringConfigHistory(int configurationVersion, String versionString, String updatedBy, LocalDateTime timestamp, String actionSummary, ScreenMonitoringConfig configSnapshot) {
        this.configurationVersion = configurationVersion;
        this.versionString = versionString;
        this.updatedBy = updatedBy;
        this.timestamp = timestamp;
        this.actionSummary = actionSummary;
        this.configSnapshot = configSnapshot;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getConfigurationVersion() { return configurationVersion; }
    public void setConfigurationVersion(int configurationVersion) { this.configurationVersion = configurationVersion; }

    public String getVersionString() { return versionString; }
    public void setVersionString(String versionString) { this.versionString = versionString; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getActionSummary() { return actionSummary; }
    public void setActionSummary(String actionSummary) { this.actionSummary = actionSummary; }

    public ScreenMonitoringConfig getConfigSnapshot() { return configSnapshot; }
    public void setConfigSnapshot(ScreenMonitoringConfig configSnapshot) { this.configSnapshot = configSnapshot; }
}
