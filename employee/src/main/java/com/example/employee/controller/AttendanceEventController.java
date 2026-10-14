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
    private final com.example.employee.service.AdminAttendanceDailyService adminAttendanceDailyService;

    public AttendanceEventController(
            AttendanceEventService attendanceEventService,
            AttendanceCalculationService attendanceCalculationService,
            com.example.employee.service.AdminAttendanceDailyService adminAttendanceDailyService
    ) {
        this.attendanceEventService = attendanceEventService;
        this.attendanceCalculationService = attendanceCalculationService;
        this.adminAttendanceDailyService = adminAttendanceDailyService;
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
            if (eventType == AttendanceEventType.WORK_ENDED) {
                // If the server says there is no active session to end,
                // we should still return 200 OK so the agent can successfully
                // terminate instead of being stuck in an active state.
                AttendanceEventDocument dummy = new AttendanceEventDocument();
                dummy.setEventId(eventId);
                dummy.setEmployeeCode(employeeCode);
                dummy.setEventType(eventType);
                return ResponseEntity.ok(dummy);
            }

            return ResponseEntity
                    .status(409)
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    eventType + " rejected. Employee already has an active work session."
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


    // =====================================================
    // Today's Daily Attendance Summary (for Agent Workspace)
    // =====================================================

    @GetMapping("/today-summary/{employeeCode}")
    public ResponseEntity<?> getTodaySummary(
            @PathVariable String employeeCode
    ) {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        com.example.employee.dto.DailyAttendanceSummary summary =
                adminAttendanceDailyService.getDailySummary(employeeCode, today);

        String firstClockInTime = "--";
        if (summary != null && summary.getTimeline() != null) {
            for (com.example.employee.dto.AttendanceTimelineItem item : summary.getTimeline()) {
                if (item.getStartedAt() != null) {
                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
                            .withZone(java.time.ZoneId.of("Asia/Kolkata"));
                    firstClockInTime = fmt.format(item.getStartedAt());
                    break;
                }
            }
        }

        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("employeeCode", employeeCode);
        resp.put("date", today.toString());
        resp.put("clockInTime", firstClockInTime);
        resp.put("workingSeconds", summary != null ? summary.getWorkingSeconds() : 0L);
        resp.put("effectiveWorkingSeconds", summary != null ? summary.getEffectiveWorkingSeconds() : 0L);
        resp.put("breakSeconds", summary != null ? summary.getBreakSeconds() : 0L);
        resp.put("lunchSeconds", summary != null ? summary.getLunchSeconds() : 0L);
        resp.put("idleSeconds", summary != null ? summary.getIdleSeconds() : 0L);

        return ResponseEntity.ok(resp);
    }
}