package org.example.activity;

import java.time.Instant;

public class ApplicationInfo {

    private final String processName;
    private final int processId;
    private final String windowTitle;

    // NEW
    private final String url;
    private final String domain;

    private final Instant startedAt;
    private final Instant endedAt;

    private final long durationSeconds;

    public ApplicationInfo(
            String processName,
            int processId,
            String windowTitle,
            String url,
            String domain,
            Instant startedAt,
            Instant endedAt,
            long durationSeconds
    ) {
        this.processName = processName;
        this.processId = processId;
        this.windowTitle = windowTitle;
        this.url = url;
        this.domain = domain;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
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

        return "ApplicationInfo{" +
                "processName='" + processName + '\'' +
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