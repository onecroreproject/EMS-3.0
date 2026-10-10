package com.example.employee.service;

import com.example.employee.attendance.AttendanceEventType;
import com.example.employee.dto.AttendanceTimelineItem;
import com.example.employee.dto.DailyAttendanceSummary;
import com.example.employee.model.AttendanceEventDocument;
import com.example.employee.repository.AttendanceEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminAttendanceDailyService {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final AttendanceEventRepository attendanceEventRepository;

    public AdminAttendanceDailyService(
            AttendanceEventRepository attendanceEventRepository
    ) {
        this.attendanceEventRepository =
                attendanceEventRepository;
    }

    public DailyAttendanceSummary getDailySummary(
            String employeeCode,
            LocalDate date
    ) {

        // =====================================================
        // Calendar day boundaries - India / IST
        // =====================================================

        Instant startOfDay =
                date.atStartOfDay(INDIA_ZONE)
                        .toInstant();

        Instant startOfNextDay =
                date.plusDays(1)
                        .atStartOfDay(INDIA_ZONE)
                        .toInstant();

        // =====================================================
        // Get events for selected calendar day
        // =====================================================

        List<AttendanceEventDocument> events =
                attendanceEventRepository.findAttendanceEventsForDay(
                        employeeCode,
                        startOfDay,
                        startOfNextDay,
                        Sort.by(
                                Sort.Direction.ASC,
                                "timestamp"
                        )
                );

        // =====================================================
        // Build normalized timeline
        // =====================================================

        List<AttendanceTimelineItem> timeline =
                new ArrayList<>();

        String currentState = null;
        Instant stateStartedAt = null;

        for (AttendanceEventDocument event : events) {

            AttendanceEventType eventType =
                    event.getEventType();

            Instant eventTime =
                    event.getTimestamp();

            // =================================================
            // WORK STARTED
            // =================================================

            if (eventType == AttendanceEventType.WORK_STARTED) {

                if (currentState != null
                        && stateStartedAt != null) {

                    addTimelineItem(
                            timeline,
                            currentState,
                            stateStartedAt,
                            eventTime
                    );
                }

                currentState = "WORKING";
                stateStartedAt = eventTime;
            }

            // =================================================
            // IDLE STARTED
            // =================================================

            else if (eventType == AttendanceEventType.IDLE_STARTED) {

                // Ignore duplicate idle events
                if ("IDLE".equals(currentState)) {
                    continue;
                }

                // Break/Lunch must not be overridden by idle
                if ("BREAK".equals(currentState)
                        || "LUNCH".equals(currentState)) {
                    continue;
                }

                if (currentState != null
                        && stateStartedAt != null) {

                    addTimelineItem(
                            timeline,
                            currentState,
                            stateStartedAt,
                            eventTime
                    );
                }

                currentState = "IDLE";
                stateStartedAt = eventTime;
            }

            // =================================================
            // IDLE ENDED
            // =================================================

            else if (eventType == AttendanceEventType.IDLE_ENDED) {

                if ("IDLE".equals(currentState)
                        && stateStartedAt != null) {

                    addTimelineItem(
                            timeline,
                            "IDLE",
                            stateStartedAt,
                            eventTime
                    );

                    currentState = "WORKING";
                    stateStartedAt = eventTime;
                }
            }

            // =================================================
            // BREAK STARTED
            // =================================================

            else if (eventType == AttendanceEventType.BREAK_STARTED) {

                if (currentState != null
                        && stateStartedAt != null) {

                    addTimelineItem(
                            timeline,
                            currentState,
                            stateStartedAt,
                            eventTime
                    );
                }

                currentState = "BREAK";
                stateStartedAt = eventTime;
            }

            // =================================================
            // BREAK ENDED
            // =================================================

            else if (eventType == AttendanceEventType.BREAK_ENDED) {

                if ("BREAK".equals(currentState)
                        && stateStartedAt != null) {

                    addTimelineItem(
                            timeline,
                            "BREAK",
                            stateStartedAt,
                            eventTime
                    );

                    currentState = "WORKING";
                    stateStartedAt = eventTime;
                }
            }

            // =================================================
            // LUNCH STARTED
            // =================================================

            else if (eventType == AttendanceEventType.LUNCH_STARTED) {

                if (currentState != null
                        && stateStartedAt != null) {

                    addTimelineItem(
                            timeline,
                            currentState,
                            stateStartedAt,
                            eventTime
                    );
                }

                currentState = "LUNCH";
                stateStartedAt = eventTime;
            }

            // =================================================
            // LUNCH ENDED
            // =================================================

            else if (eventType == AttendanceEventType.LUNCH_ENDED) {

                if ("LUNCH".equals(currentState)
                        && stateStartedAt != null) {

                    addTimelineItem(
                            timeline,
                            "LUNCH",
                            stateStartedAt,
                            eventTime
                    );

                    currentState = "WORKING";
                    stateStartedAt = eventTime;
                }
            }

            // =================================================
            // WORK ENDED
            // =================================================

            else if (eventType == AttendanceEventType.WORK_ENDED) {

                if (currentState != null
                        && stateStartedAt != null) {

                    addTimelineItem(
                            timeline,
                            currentState,
                            stateStartedAt,
                            eventTime
                    );
                }

                currentState = null;
                stateStartedAt = null;
            }
        }

        // =====================================================
        // Close an open state
        // =====================================================

        if (currentState != null
                && stateStartedAt != null) {

            Instant now = Instant.now();

            Instant endTime =
                    now.isAfter(startOfNextDay)
                            ? startOfNextDay
                            : now;

            if (endTime.isAfter(stateStartedAt)) {

                addTimelineItem(
                        timeline,
                        currentState,
                        stateStartedAt,
                        endTime
                );
            }
        }

        // =====================================================
        // Calculate daily totals from normalized timeline
        // =====================================================

        long workingSeconds = 0;
        long idleSeconds = 0;
        long breakSeconds = 0;
        long lunchSeconds = 0;

        for (AttendanceTimelineItem item : timeline) {

            long duration =
                    item.getDurationSeconds();

            switch (item.getType()) {

                case "WORKING":
                    workingSeconds += duration;
                    break;

                case "IDLE":
                    idleSeconds += duration;
                    break;

                case "BREAK":
                    breakSeconds += duration;
                    break;

                case "LUNCH":
                    lunchSeconds += duration;
                    break;

                default:
                    break;
            }
        }

        /*
         * IMPORTANT:
         *
         * This follows the current Admin timeline model:
         * WORKING represents actual working-state intervals.
         *
         * Therefore idle/break/lunch are already outside
         * workingSeconds and must NOT be subtracted again.
         */
        long effectiveWorkingSeconds =
                workingSeconds;

        return new DailyAttendanceSummary(
                employeeCode,
                date.toString(),
                workingSeconds,
                idleSeconds,
                breakSeconds,
                lunchSeconds,
                effectiveWorkingSeconds,
                timeline
        );
    }

    // =========================================================
    // Add timeline item
    // =========================================================

    private void addTimelineItem(
            List<AttendanceTimelineItem> timeline,
            String type,
            Instant startedAt,
            Instant endedAt
    ) {

        if (startedAt == null
                || endedAt == null) {
            return;
        }

        if (!endedAt.isAfter(startedAt)) {
            return;
        }

        long durationSeconds =
                Duration.between(
                        startedAt,
                        endedAt
                ).getSeconds();

        if (durationSeconds <= 0) {
            return;
        }

        // Merge consecutive same states
        if (!timeline.isEmpty()) {

            AttendanceTimelineItem previous =
                    timeline.get(
                            timeline.size() - 1
                    );

            if (previous.getType().equals(type)
                    && previous.getEndedAt().equals(startedAt)) {

                previous.setEndedAt(endedAt);

                previous.setDurationSeconds(
                        previous.getDurationSeconds()
                                + durationSeconds
                );

                return;
            }
        }

        timeline.add(
                new AttendanceTimelineItem(
                        type,
                        startedAt,
                        endedAt,
                        durationSeconds
                )
        );
    }
}