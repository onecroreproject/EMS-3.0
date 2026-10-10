package org.example.attendance;

import java.time.LocalDateTime;

public class AttendanceEventFactory {

    private final String employeeCode;
    private final String deviceId;

    public AttendanceEventFactory(
            String employeeCode,
            String deviceId
    ) {
        this.employeeCode = employeeCode;
        this.deviceId = deviceId;
    }

    public AttendanceEvent create(AttendanceEventType eventType) {

        return new AttendanceEvent(
                employeeCode,
                deviceId,
                eventType,
                LocalDateTime.now()
        );
    }
}