package com.example.employee.dto;

import java.util.List;

public class DailyAttendanceSummary {

    private String employeeCode;
    private String date;

    private long workingSeconds;
    private long idleSeconds;
    private long breakSeconds;
    private long lunchSeconds;
    private long effectiveWorkingSeconds;

    private List<AttendanceTimelineItem> timeline;

    public DailyAttendanceSummary() {
    }

    public DailyAttendanceSummary(
            String employeeCode,
            String date,
            long workingSeconds,
            long idleSeconds,
            long breakSeconds,
            long lunchSeconds,
            long effectiveWorkingSeconds,
            List<AttendanceTimelineItem> timeline
    ) {
        this.employeeCode = employeeCode;
        this.date = date;
        this.workingSeconds = workingSeconds;
        this.idleSeconds = idleSeconds;
        this.breakSeconds = breakSeconds;
        this.lunchSeconds = lunchSeconds;
        this.effectiveWorkingSeconds = effectiveWorkingSeconds;
        this.timeline = timeline;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getWorkingSeconds() {
        return workingSeconds;
    }

    public void setWorkingSeconds(long workingSeconds) {
        this.workingSeconds = workingSeconds;
    }

    public long getIdleSeconds() {
        return idleSeconds;
    }

    public void setIdleSeconds(long idleSeconds) {
        this.idleSeconds = idleSeconds;
    }

    public long getBreakSeconds() {
        return breakSeconds;
    }

    public void setBreakSeconds(long breakSeconds) {
        this.breakSeconds = breakSeconds;
    }

    public long getLunchSeconds() {
        return lunchSeconds;
    }

    public void setLunchSeconds(long lunchSeconds) {
        this.lunchSeconds = lunchSeconds;
    }

    public long getEffectiveWorkingSeconds() {
        return effectiveWorkingSeconds;
    }

    public void setEffectiveWorkingSeconds(long effectiveWorkingSeconds) {
        this.effectiveWorkingSeconds = effectiveWorkingSeconds;
    }

    public List<AttendanceTimelineItem> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<AttendanceTimelineItem> timeline) {
        this.timeline = timeline;
    }
}
