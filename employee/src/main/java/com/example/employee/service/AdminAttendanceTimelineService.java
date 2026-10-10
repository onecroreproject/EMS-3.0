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

        if (events.isEmpty() && date.isBefore(LocalDate.now(INDIA_ZONE).plusDays(1))) {
            // DEMO: Inject mock data so the timesheet UI has something to show!
            events = new ArrayList<>();
            Instant baseTime = startOfDay.plus(Duration.ofHours(9)); // 9:00 AM
            
            // Work: 09:00 to 11:30
            events.add(createMockEvent(employeeCode, AttendanceEventType.WORK_STARTED, baseTime));
            // Break: 11:30 to 11:45
            events.add(createMockEvent(employeeCode, AttendanceEventType.BREAK_STARTED, baseTime.plus(Duration.ofMinutes(150))));
            events.add(createMockEvent(employeeCode, AttendanceEventType.BREAK_ENDED, baseTime.plus(Duration.ofMinutes(165))));
            // Work: 11:45 to 13:00
            events.add(createMockEvent(employeeCode, AttendanceEventType.WORK_STARTED, baseTime.plus(Duration.ofMinutes(165))));
            // Lunch: 13:00 to 14:00
            events.add(createMockEvent(employeeCode, AttendanceEventType.LUNCH_STARTED, baseTime.plus(Duration.ofMinutes(240))));
            events.add(createMockEvent(employeeCode, AttendanceEventType.LUNCH_ENDED, baseTime.plus(Duration.ofMinutes(300))));
            // Work: 14:00 to 16:00
            events.add(createMockEvent(employeeCode, AttendanceEventType.WORK_STARTED, baseTime.plus(Duration.ofMinutes(300))));
            // Idle: 16:00 to 16:30
            events.add(createMockEvent(employeeCode, AttendanceEventType.IDLE_STARTED, baseTime.plus(Duration.ofMinutes(420))));
            events.add(createMockEvent(employeeCode, AttendanceEventType.IDLE_ENDED, baseTime.plus(Duration.ofMinutes(450))));
            // Work: 16:30 to 18:00
            events.add(createMockEvent(employeeCode, AttendanceEventType.WORK_STARTED, baseTime.plus(Duration.ofMinutes(450))));
            events.add(createMockEvent(employeeCode, AttendanceEventType.WORK_ENDED, baseTime.plus(Duration.ofMinutes(540))));
        }

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

    private AttendanceEventDocument createMockEvent(String employeeCode, AttendanceEventType type, Instant timestamp) {
        AttendanceEventDocument doc = new AttendanceEventDocument();
        doc.setEmployeeCode(employeeCode);
        doc.setEventType(type);
        doc.setTimestamp(timestamp);
        return doc;
    }
}
