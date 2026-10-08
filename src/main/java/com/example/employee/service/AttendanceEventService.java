package com.example.employee.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.employee.attendance.AttendanceEventType;
import com.example.employee.model.AttendanceEventDocument;
import com.example.employee.repository.AttendanceEventRepository;

@Service
public class AttendanceEventService {

    private final AttendanceEventRepository repository;

    public AttendanceEventService(
            AttendanceEventRepository repository) {

        this.repository = repository;
    }

    public AttendanceEventDocument recordEvent(
            String eventId,
            String employeeCode,
            String deviceId,
            AttendanceEventType eventType,
            Instant timestamp
    ) {

        /*
         * ---------------------------------------------------------
         * 1. Check whether this exact event was already processed.
         * ---------------------------------------------------------
         */
        if (eventId != null && !eventId.isBlank()) {

            var existingEvent =
                    repository.findByEventId(eventId);

            if (existingEvent.isPresent()) {

                System.out.println(
                        "Duplicate attendance event detected. "
                                + "Event already exists: "
                                + eventId
                );

                return existingEvent.get();
            }
        }

        /*
         * ---------------------------------------------------------
         * 2. Use server time when timestamp is not provided.
         * ---------------------------------------------------------
         */
        Instant eventTime =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        /*
         * ---------------------------------------------------------
         * 3. Protect against duplicate WORK_STARTED events.
         * ---------------------------------------------------------
         */
        if (eventType == AttendanceEventType.WORK_STARTED) {

            boolean workAlreadyStarted =
                    hasOpenWorkSession(employeeCode);

            if (workAlreadyStarted) {

                System.out.println(
                        "WORK_STARTED ignored. "
                                + "Employee already has an open "
                                + "work session: "
                                + employeeCode
                );

                return null;
            }
        }

        /*
         * ---------------------------------------------------------
         * 4. Protect against invalid WORK_ENDED events.
         * ---------------------------------------------------------
         */
        if (eventType == AttendanceEventType.WORK_ENDED) {

            boolean workAlreadyStarted =
                    hasOpenWorkSession(employeeCode);

            if (!workAlreadyStarted) {

                System.out.println(
                        "WORK_ENDED ignored. "
                                + "No open work session found "
                                + "for employee: "
                                + employeeCode
                );

                return null;
            }
        }

        /*
         * ---------------------------------------------------------
         * 5. Create and save the attendance event.
         * ---------------------------------------------------------
         */
        AttendanceEventDocument event =
                new AttendanceEventDocument();

        event.setEventId(eventId);
        event.setEmployeeCode(employeeCode);
        event.setDeviceId(deviceId);
        event.setEventType(eventType);
        event.setTimestamp(eventTime);

        return repository.save(event);
    }

    public List<AttendanceEventDocument> getEmployeeEvents(
            String employeeCode) {

        return repository
                .findByEmployeeCodeOrderByTimestampAsc(
                        employeeCode
                );
    }

    /*
     * =========================================================
     * Get Current Employee Activity
     * =========================================================
     *
     * The current activity is determined from the employee's
     * latest attendance event.
     *
     * Example:
     *
     * WORK_STARTED    -> WORKING
     * IDLE_STARTED    -> IDLE
     * IDLE_ENDED      -> WORKING
     * BREAK_STARTED   -> BREAK
     * BREAK_ENDED     -> WORKING
     * LUNCH_STARTED   -> LUNCH
     * LUNCH_ENDED     -> WORKING
     * MEETING_STARTED -> MEETING
     * MEETING_ENDED   -> WORKING
     * WORK_ENDED      -> CLOCKED_OUT
     * CLOCK_OUT       -> CLOCKED_OUT
     *
     * If no attendance event exists, the employee is considered
     * OFFLINE.
     */
    public String getCurrentActivity(String employeeCode) {

        Optional<AttendanceEventDocument> latestEvent =
                repository.findTopByEmployeeCodeOrderByTimestampDesc(
                        employeeCode
                );

        if (latestEvent.isEmpty()) {
            return "OFFLINE";
        }

        AttendanceEventType eventType =
                latestEvent.get().getEventType();

        return switch (eventType) {

            case WORK_STARTED ->
                    "WORKING";

            case IDLE_STARTED ->
                    "IDLE";

            case IDLE_ENDED ->
                    "WORKING";

            case BREAK_STARTED ->
                    "BREAK";

            case BREAK_ENDED ->
                    "WORKING";

            case LUNCH_STARTED ->
                    "LUNCH";

            case LUNCH_ENDED ->
                    "WORKING";

            case MEETING_STARTED ->
                    "MEETING";

            case MEETING_ENDED ->
                    "WORKING";

            case WORK_ENDED ->
                    "CLOCKED_OUT";

            case CLOCK_IN ->
                    "WORKING";

            case CLOCK_OUT ->
                    "CLOCKED_OUT";
        };
    }

    /*
     * =========================================================
     * Check whether employee currently has an open work session
     * =========================================================
     */
    private boolean hasOpenWorkSession(
            String employeeCode) {

        List<AttendanceEventDocument> events =
                repository
                        .findByEmployeeCodeOrderByTimestampAsc(
                                employeeCode
                        );

        boolean workStarted = false;

        for (AttendanceEventDocument event : events) {

            if (event.getEventType()
                    == AttendanceEventType.WORK_STARTED) {

                workStarted = true;

            } else if (event.getEventType()
                    == AttendanceEventType.WORK_ENDED) {

                workStarted = false;
            }
        }

        return workStarted;
    }
}