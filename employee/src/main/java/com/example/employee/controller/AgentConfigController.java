package com.example.employee.controller;

import com.example.employee.model.ScreenMonitoringConfig;
import com.example.employee.service.ScreenMonitoringConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/config")
public class AgentConfigController {

    @Value("${workday.required-hours:9}")
    private int requiredHours;

    @Value("${idle.grace-minutes:2}")
    private int idleGraceMinutes;

    @Value("${idle.auto-work-end-minutes:60}")
    private int autoWorkEndMinutes;

    @Value("${agent.heartbeat-interval-seconds:10}")
    private int heartbeatIntervalSeconds;

    @Value("${agent.login-reminder-seconds:60}")
    private int loginReminderIntervalSeconds;

    private final ScreenMonitoringConfigService screenMonitoringConfigService;

    public AgentConfigController(ScreenMonitoringConfigService screenMonitoringConfigService) {
        this.screenMonitoringConfigService = screenMonitoringConfigService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAgentConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("requiredHours", requiredHours);
        config.put("idleGraceMinutes", idleGraceMinutes);
        config.put("autoWorkEndMinutes", autoWorkEndMinutes);
        config.put("heartbeatIntervalSeconds", heartbeatIntervalSeconds);
        config.put("loginReminderIntervalSeconds", loginReminderIntervalSeconds);

        try {
            ScreenMonitoringConfig sc = screenMonitoringConfigService.getConfig();
            if (sc != null) {
                config.put("requiredHours", sc.getDailyRequiredWorkingHours());
                config.put("idleGraceMinutes", sc.getIdleDetectionThreshold());
                config.put("autoWorkEndMinutes", sc.getMarkOfflineAfterContinuousIdle());
                config.put("markOfflineAfterContinuousIdle", sc.getMarkOfflineAfterContinuousIdle());
                config.put("enableAutomaticOfflineStatus", sc.isEnableAutomaticOfflineStatus());

                // Screen Capture dynamic settings for Agent
                config.put("captureEnabled", sc.isCaptureEnabled());
                config.put("captureInterval", sc.getCaptureInterval()); // in minutes
                config.put("imageQuality", sc.getImageQuality()); // 10-100
                config.put("imageFormat", sc.getImageFormat()); // JPEG/PNG
                config.put("captureDuringWorking", sc.isCaptureDuringWorking());
                config.put("captureDuringIdle", sc.isCaptureDuringIdle());
                config.put("captureDuringBreak", sc.isCaptureDuringBreak());
                config.put("captureWhenLoggedOut", sc.isCaptureWhenLoggedOut());

                // Offline & Sync
                config.put("enableOfflineCapture", sc.isEnableOfflineCapture());
                config.put("localStorageLimit", sc.getLocalStorageLimit());
                config.put("retryInterval", sc.getRetryInterval());
                config.put("maxRetryAttempts", sc.getMaxRetryAttempts());
                config.put("autoSyncOnStartup", sc.isAutoSyncOnStartup());

                config.put("configurationVersion", sc.getConfigurationVersion());
                config.put("versionString", sc.getVersionString());
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(config);
    }
}

