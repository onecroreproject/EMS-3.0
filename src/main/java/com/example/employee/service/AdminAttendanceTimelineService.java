package com.example.employee.service;

import com.example.employee.attendance.AttendanceEventType;
import com.example.employee.dto.AttendanceTimelineItem;
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
public class AdminAttendanceTimelineService {

    private final AttendanceEventRepository attendanceEventRepository;

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");


    public AdminAttendanceTimelineService(
            AttendanceEventRepository attendanceEventRepository
    ) {
        this.attendanceEventRepository =
                attendanceEventRepository;
    }


    // =====================================================
    // Generate Activity Timeline
    // =====================================================

    public List<AttendanceTimelineItem> getTimeline(
            String employeeCode,
            LocalDate date
    ) {

        Instant startOfDay =
                date.atStartOfDay(INDIA_ZONE)
                        .toInstant();

        Instant startOfNextDay =
                date.plusDays(1)
                        .atStartOfDay(INDIA_ZONE)
                        .toInstant();


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

                /*
                 * If another state is already active,
                 * close it before starting WORKING.
                 */

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

                /*
                 * Ignore duplicate IDLE_STARTED events.
                 */

                if ("IDLE".equals(currentState)) {
                    continue;
                }


                /*
                 * If BREAK or LUNCH is active,
                 * IDLE_STARTED must not override it.
                 */

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
        // If employee is still working at the end of the
        // selected day, close the state at current time.
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


        return timeline;
    }


    // =====================================================
    // Add Timeline Item
    // =====================================================

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


        /*
         * Merge consecutive states of the same type.
         *
         * This prevents duplicate small segments from
         * making the Admin timeline unnecessarily noisy.
         */

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
