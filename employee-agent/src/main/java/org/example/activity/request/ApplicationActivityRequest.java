package org.example.activity.request;

import org.example.activity.ApplicationInfo;

import java.time.Instant;

public class ApplicationActivityRequest {

    private String employeeCode;
    private String deviceId;

    private String processName;
    private int processId;
    private String windowTitle;

    // NEW
    private String url;
    private String domain;

    private Instant startedAt;
    private Instant endedAt;

    private long durationSeconds;

    public ApplicationActivityRequest(
            String employeeCode,
            String deviceId,
            ApplicationInfo activity
    ) {

        this.employeeCode = employeeCode;
        this.deviceId = deviceId;

        this.processName = activity.getProcessName();
        this.processId = activity.getProcessId();
        this.windowTitle = activity.getWindowTitle();

        // NEW
        this.url = activity.getUrl();
        this.domain = activity.getDomain();

        this.startedAt = activity.getStartedAt();
        this.endedAt = activity.getEndedAt();

        this.durationSeconds =
                activity.getDurationSeconds();
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getProcessName() {
        return processName;
    }

    public int getProcessId() {
        return processId;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    // NEW
    public String getUrl() {
        return url;
    }

    // NEW
    public String getDomain() {
        return domain;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    @Override
    public String toString() {

        return "ApplicationActivityRequest{" +
                "employeeCode='" + employeeCode + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", processName='" + processName + '\'' +
                ", processId=" + processId +
                ", windowTitle='" + windowTitle + '\'' +
                ", url='" + url + '\'' +
                ", domain='" + domain + '\'' +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                ", durationSeconds=" + durationSeconds +
                '}';
    }
}