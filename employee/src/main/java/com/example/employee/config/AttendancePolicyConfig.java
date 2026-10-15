package com.example.employee.config;

import com.example.employee.model.ScreenMonitoringConfig;
import com.example.employee.repository.ScreenMonitoringConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AttendancePolicyConfig {

    @Value("${workday.required-hours:9}")
    private int requiredWorkHours;

    @Value("${idle.grace-minutes:2}")
    private int idleGraceMinutes;

    @Autowired(required = false)
    private ScreenMonitoringConfigRepository screenMonitoringConfigRepository;

    public int getRequiredWorkHours() {
        if (screenMonitoringConfigRepository != null) {
            try {
                Optional<ScreenMonitoringConfig> configOpt = screenMonitoringConfigRepository.findFirstByOrderByIdAsc();
                if (configOpt.isPresent()) {
                    return configOpt.get().getDailyRequiredWorkingHours();
                }
            } catch (Exception ignored) {}
        }
        return requiredWorkHours;
    }

    public int getIdleGraceMinutes() {
        if (screenMonitoringConfigRepository != null) {
            try {
                Optional<ScreenMonitoringConfig> configOpt = screenMonitoringConfigRepository.findFirstByOrderByIdAsc();
                if (configOpt.isPresent()) {
                    return configOpt.get().getIdleDetectionThreshold();
                }
            } catch (Exception ignored) {}
        }
        return idleGraceMinutes;
    }
}