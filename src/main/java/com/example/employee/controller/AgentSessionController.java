package com.example.employee.controller;

import com.example.employee.dto.AgentSessionStartRequest;
import com.example.employee.model.EmployeeSession;
import com.example.employee.service.EmployeeSessionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/agent/session")
public class AgentSessionController {

    private final EmployeeSessionService sessionService;

    public AgentSessionController(
            EmployeeSessionService sessionService) {

        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startSession(
            @RequestBody AgentSessionStartRequest request) {

        Map<String, Object> response = new HashMap<>();

        // Validate request
        if (request.getEmployeeId() == null ||
                request.getEmployeeId().trim().isEmpty() ||
                request.getDeviceId() == null ||
                request.getDeviceId().trim().isEmpty()) {

            response.put("success", false);
            response.put(
                    "message",
                    "Employee ID and device ID are required"
            );

            return ResponseEntity.badRequest().body(response);
        }

        // Check existing active session
        Optional<EmployeeSession> activeSession =
                sessionService.findActiveSession(
                        request.getEmployeeId()
                );

        if (activeSession.isPresent()) {

            EmployeeSession existing =
                    activeSession.get();

            // Same device
            if (existing.getDeviceId()
                    .equals(request.getDeviceId())) {

                response.put("success", true);
                response.put(
                        "message",
                        "Session already active on this device"
                );
                response.put(
                        "deviceId",
                        request.getDeviceId()
                );

                return ResponseEntity.ok(response);
            }

            // Different device
            response.put("success", false);
            response.put(
                    "message",
                    "This employee is already logged in on another device."
            );

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(response);
        }

        // Create new session
        EmployeeSession session =
                sessionService.createSession(
                        request.getEmployeeId(),
                        request.getEmployeeCode(),
                        request.getDeviceId()
                );

        response.put("success", true);
        response.put(
                "message",
                "Employee session started successfully"
        );
        response.put(
                "sessionId",
                session.getId()
        );
        response.put(
                "deviceId",
                session.getDeviceId()
        );
        response.put(
                "loginAt",
                session.getLoginAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestParam String deviceId) {

        Map<String, Object> response = new HashMap<>();

        if (deviceId == null ||
                deviceId.trim().isEmpty()) {

            response.put("success", false);
            response.put(
                    "message",
                    "Device ID is required"
            );

            return ResponseEntity.badRequest().body(response);
        }

        sessionService.logout(deviceId);

        response.put("success", true);
        response.put(
                "message",
                "Employee session ended successfully"
        );
        response.put("deviceId", deviceId);

        return ResponseEntity.ok(response);
    }
}