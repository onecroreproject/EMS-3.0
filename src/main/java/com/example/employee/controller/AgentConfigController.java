package com.example.employee.controller;

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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAgentConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("requiredHours", requiredHours);
        config.put("idleGraceMinutes", idleGraceMinutes);
        config.put("autoWorkEndMinutes", autoWorkEndMinutes);
        config.put("heartbeatIntervalSeconds", heartbeatIntervalSeconds);
        config.put("loginReminderIntervalSeconds", loginReminderIntervalSeconds);
        return ResponseEntity.ok(config);
    }
}
