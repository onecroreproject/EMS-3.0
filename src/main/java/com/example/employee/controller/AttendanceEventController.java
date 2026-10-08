package com.example.employee.controller;

import java.time.Instant;

import com.example.employee.service.AttendanceCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.employee.attendance.AttendanceEventType;
import com.example.employee.model.AttendanceEventDocument;
import com.example.employee.service.AttendanceEventService;

import java.util.Map;

@RestController
@RequestMapping("/api/agent/attendance")
public class AttendanceEventController {

    private final AttendanceEventService attendanceEventService;
    private final AttendanceCalculationService attendanceCalculationService;

    public AttendanceEventController(
            AttendanceEventService attendanceEventService,
            AttendanceCalculationService attendanceCalculationService
    ) {
        this.attendanceEventService = attendanceEventService;
        this.attendanceCalculationService = attendanceCalculationService;
    }


    // =====================================================
    // Record Attendance Event
    // =====================================================

    @PostMapping("/event")
    public ResponseEntity<?> recordEvent(
            @RequestParam String eventId,
            @RequestParam String employeeCode,
            @RequestParam String deviceId,
            @RequestParam AttendanceEventType eventType,
            @RequestParam(required = false) String timestamp
    ) {

        Instant eventTime =
                timestamp != null
                        ? Instant.parse(timestamp)
                        : Instant.now();

        AttendanceEventDocument savedEvent =
                attendanceEventService.recordEvent(
                        eventId,
                        employeeCode,
                        deviceId,
                        eventType,
                        eventTime
                );

        if (savedEvent == null) {

            return ResponseEntity
                    .status(409)
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    eventType
                                            + " rejected. "
                                            + "Employee already has an active work session."
                            )
                    );
        }

        return ResponseEntity.ok(savedEvent);
    }


    // =====================================================
    // Idle Duration
    // =====================================================

    @GetMapping("/idle/{employeeCode}")
    public ResponseEntity<Long> getIdleDuration(
            @PathVariable String employeeCode
    ) {

        long idleSeconds =
                attendanceCalculationService
                        .calculateIdleSeconds(employeeCode);

        return ResponseEntity.ok(
                idleSeconds
        );
    }


    // =====================================================
    // Working Duration
    // =====================================================

    @GetMapping("/working/{employeeCode}")
    public ResponseEntity<Long> getWorkingDuration(
            @PathVariable String employeeCode
    ) {

        long workingSeconds =
                attendanceCalculationService
                        .calculateWorkingSeconds(employeeCode);

        return ResponseEntity.ok(
                workingSeconds
        );
    }


    // =====================================================
    // Effective Work Duration
    // =====================================================

    @GetMapping("/effective/{employeeCode}")
    public ResponseEntity<Long> getEffectiveWorkDuration(
            @PathVariable String employeeCode
    ) {

        long effectiveWorkSeconds =
                attendanceCalculationService
                        .calculateEffectiveWorkSeconds(
                                employeeCode
                        );

        return ResponseEntity.ok(
                effectiveWorkSeconds
        );
    }


    // =====================================================
    // Break Duration
    // =====================================================

    @GetMapping("/break/{employeeCode}")
    public ResponseEntity<Long> getBreakDuration(
            @PathVariable String employeeCode
    ) {

        long breakSeconds =
                attendanceCalculationService
                        .calculateBreakSeconds(
                                employeeCode
                        );

        return ResponseEntity.ok(
                breakSeconds
        );
    }


    // =====================================================
    // Required Work Duration
    // =====================================================

    @GetMapping("/required")
    public ResponseEntity<Long> getRequiredWorkDuration() {

        long requiredWorkSeconds =
                attendanceCalculationService
                        .calculateRequiredWorkSeconds();

        return ResponseEntity.ok(
                requiredWorkSeconds
        );
    }


    // =====================================================
    // Remaining Work Duration
    // =====================================================

    @GetMapping("/remaining/{employeeCode}")
    public ResponseEntity<Long> getRemainingWorkDuration(
            @PathVariable String employeeCode
    ) {

        long remainingWorkSeconds =
                attendanceCalculationService
                        .calculateRemainingWorkSeconds(
                                employeeCode
                        );

        return ResponseEntity.ok(
                remainingWorkSeconds
        );
    }
}