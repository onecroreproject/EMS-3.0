package com.example.employee.service;

import com.example.employee.attendance.AttendanceEventType;
import com.example.employee.config.AttendancePolicyConfig;
import com.example.employee.model.AttendanceEventDocument;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AttendanceCalculationService {

    private final AttendanceEventService attendanceEventService;
    private final AttendancePolicyConfig attendancePolicyConfig;

    public AttendanceCalculationService(
            AttendanceEventService attendanceEventService,
            AttendancePolicyConfig attendancePolicyConfig) {

        this.attendanceEventService = attendanceEventService;
        this.attendancePolicyConfig = attendancePolicyConfig;
    }


    // =====================================================
    // Idle Duration
    // =====================================================

    public long calculateIdleSeconds(String employeeCode) {

        List<AttendanceEventDocument> events =
                attendanceEventService
                        .getEmployeeEvents(employeeCode);

        long totalIdleSeconds = 0;

        AttendanceEventDocument idleStart = null;

        for (AttendanceEventDocument event : events) {

            if (event.getEventType()
                    == AttendanceEventType.IDLE_STARTED) {

                idleStart = event;
            }

            else if (event.getEventType()
                    == AttendanceEventType.IDLE_ENDED
                    && idleStart != null) {

                long seconds =
                        Duration.between(
                                idleStart.getTimestamp(),
                                event.getTimestamp()
                        ).getSeconds();

                totalIdleSeconds += seconds;

                idleStart = null;
            }
        }

        /*
         * Employee is currently idle.
         * Calculate the open idle period up to now.
         */
        if (idleStart != null) {

            long seconds =
                    Duration.between(
                            idleStart.getTimestamp(),
                            Instant.now()
                    ).getSeconds();

            totalIdleSeconds += seconds;
        }

        return totalIdleSeconds;
    }


    // =====================================================
    // Working Duration
    // =====================================================

    public long calculateWorkingSeconds(String employeeCode) {

        List<AttendanceEventDocument> events =
                attendanceEventService
                        .getEmployeeEvents(employeeCode);

        long totalWorkingSeconds = 0;

        AttendanceEventDocument workStart = null;

        for (AttendanceEventDocument event : events) {

            if (event.getEventType()
                    == AttendanceEventType.WORK_STARTED) {

                workStart = event;
            }

            else if (event.getEventType()
                    == AttendanceEventType.WORK_ENDED
                    && workStart != null) {

                long seconds =
                        Duration.between(
                                workStart.getTimestamp(),
                                event.getTimestamp()
                        ).getSeconds();

                totalWorkingSeconds += seconds;

                workStart = null;
            }
        }

        /*
         * Employee is currently working.
         * Calculate the open work period up to now.
         */
        if (workStart != null) {

            long seconds =
                    Duration.between(
                            workStart.getTimestamp(),
                            Instant.now()
                    ).getSeconds();

            totalWorkingSeconds += seconds;
        }

        return totalWorkingSeconds;
    }


    // =====================================================
    // Effective Work Duration
    // =====================================================

    public long calculateEffectiveWorkSeconds(
            String employeeCode) {

        long workingSeconds =
                calculateWorkingSeconds(
                        employeeCode
                );

        long idleSeconds =
                calculateIdleSeconds(
                        employeeCode
                );

        long breakSeconds =
                calculateBreakSeconds(
                        employeeCode
                );

        long effectiveWorkSeconds =
                workingSeconds
                        - idleSeconds
                        - breakSeconds;

        return Math.max(
                effectiveWorkSeconds,
                0
        );
    }


    // =====================================================
    // Required Work Duration
    // =====================================================

    public long calculateRequiredWorkSeconds() {

        int requiredHours =
                attendancePolicyConfig
                        .getRequiredWorkHours();

        return requiredHours * 60L * 60L;
    }


    // =====================================================
    // Remaining Work Duration
    // =====================================================

    public long calculateRemainingWorkSeconds(
            String employeeCode) {

        long requiredWorkSeconds =
                calculateRequiredWorkSeconds();

        long effectiveWorkSeconds =
                calculateEffectiveWorkSeconds(
                        employeeCode
                );

        long remainingWorkSeconds =
                requiredWorkSeconds
                        - effectiveWorkSeconds;

        /*
         * Remaining work cannot be negative.
         */
        return Math.max(
                remainingWorkSeconds,
                0
        );
    }


    // =====================================================
    // Break Duration
    // =====================================================

    public long calculateBreakSeconds(
            String employeeCode) {

        List<AttendanceEventDocument> events =
                attendanceEventService
                        .getEmployeeEvents(employeeCode);

        long totalBreakSeconds = 0;

        AttendanceEventDocument breakStart = null;

        for (AttendanceEventDocument event : events) {

            if (event.getEventType()
                    == AttendanceEventType.BREAK_STARTED) {

                breakStart = event;
            }

            else if (event.getEventType()
                    == AttendanceEventType.BREAK_ENDED
                    && breakStart != null) {

                long seconds =
                        Duration.between(
                                breakStart.getTimestamp(),
                                event.getTimestamp()
                        ).getSeconds();

                totalBreakSeconds += seconds;

                breakStart = null;
            }
        }

        /*
         * Employee is currently on break.
         * Calculate the open break period up to now.
         */
        if (breakStart != null) {

            long seconds =
                    Duration.between(
                            breakStart.getTimestamp(),
                            Instant.now()
                    ).getSeconds();

            totalBreakSeconds += seconds;
        }

        return totalBreakSeconds;
    }
}