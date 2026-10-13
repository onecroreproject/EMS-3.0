package com.example.employee.controller;

import com.example.employee.dto.AgentSessionStartRequest;
import com.example.employee.model.Employee;
import com.example.employee.model.EmployeeSession;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.EmployeeSessionRepository;
import com.example.employee.service.EmployeeSessionService;
import com.example.employee.service.NotificationService;

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
    private final EmployeeSessionRepository sessionRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    public AgentSessionController(
            EmployeeSessionService sessionService,
            EmployeeSessionRepository sessionRepository,
            EmployeeRepository employeeRepository,
            NotificationService notificationService) {

        this.sessionService = sessionService;
        this.sessionRepository = sessionRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
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

        // Live dynamic notification for Agent Session Start
        try {
            String name = (request.getEmployeeCode() != null) ? request.getEmployeeCode() : "Employee";
            if (request.getEmployeeId() != null) {
                Optional<Employee> empOpt = employeeRepository.findById(request.getEmployeeId());
                if (empOpt.isPresent()) {
                    name = empOpt.get().getName();
                }
            }
            String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
            notificationService.createNotification(
                "Employee Login",
                name + (request.getEmployeeCode() != null ? " (" + request.getEmployeeCode() + ")" : "") + " started work session at " + timeStr + ".",
                "Employee",
                "login",
                "/employees/view"
            );
        } catch (Exception ignored) {}

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

        // Live dynamic notification for Employee Offline
        try {
            sessionRepository.findByDeviceIdAndStatus(deviceId, "ACTIVE").ifPresent(activeSession -> {
                String empCode = activeSession.getEmployeeCode();
                String empId = activeSession.getEmployeeId();
                String name = (empCode != null && !empCode.isEmpty()) ? empCode : "Employee";
                if (empId != null) {
                    Optional<Employee> empOpt = employeeRepository.findById(empId);
                    if (empOpt.isPresent()) {
                        name = empOpt.get().getName();
                    }
                }
                String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
                notificationService.createNotification(
                    "Employee Went Offline",
                    name + (empCode != null && !empCode.isEmpty() ? " (" + empCode + ")" : "") + " went offline. Last active at " + timeStr + ".",
                    "Employee",
                    "offline",
                    "/employees/view"
                );
            });
        } catch (Exception ignored) {}

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