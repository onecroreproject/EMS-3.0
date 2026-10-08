package com.example.employee.dto;

import java.time.Instant;

public class AttendanceTimelineItem {

    private String type;

    private Instant startedAt;

    private Instant endedAt;

    private long durationSeconds;


    public AttendanceTimelineItem() {
    }


    public AttendanceTimelineItem(
            String type,
            Instant startedAt,
            Instant endedAt,
            long durationSeconds
    ) {
        this.type = type;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }


    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }


    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
