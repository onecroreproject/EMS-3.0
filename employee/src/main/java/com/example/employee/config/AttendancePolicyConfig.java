package com.example.employee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AttendancePolicyConfig {

    @Value("${workday.required-hours:9}")
    private int requiredWorkHours;

    @Value("${idle.grace-minutes:2}")
    private int idleGraceMinutes;

    public int getRequiredWorkHours() {
        return requiredWorkHours;
    }

    public int getIdleGraceMinutes() {
        return idleGraceMinutes;
    }
}