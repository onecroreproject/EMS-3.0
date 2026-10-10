package org.example.attendance;

import java.time.LocalDateTime;
import java.util.UUID;

public class AttendanceEvent {

    private final String eventId;
    private final String employeeCode;
    private final String deviceId;
    private final AttendanceEventType eventType;
    private final LocalDateTime timestamp;

    /**
     * Constructor for creating a new attendance event.
     * A unique event ID is generated automatically.
     */
    public AttendanceEvent(
            String employeeCode,
            String deviceId,
            AttendanceEventType eventType,
            LocalDateTime timestamp
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.employeeCode = employeeCode;
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    /**
     * Constructor for loading an existing event
     * from the persistent queue.
     *
     * The original event ID is preserved.
     */
    public AttendanceEvent(
            String eventId,
            String employeeCode,
            String deviceId,
            AttendanceEventType eventType,
            LocalDateTime timestamp
    ) {
        this.eventId = eventId;
        this.employeeCode = employeeCode;
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public AttendanceEventType getEventType() {
        return eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}