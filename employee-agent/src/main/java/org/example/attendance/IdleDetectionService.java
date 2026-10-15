package org.example.attendance;

import org.example.activity.UserActivityMonitor;
import org.example.config.AgentConfig;

public class IdleDetectionService {

    private final UserActivityMonitor activityMonitor;
    private final AgentConfig config;
    private int fallbackIdleGraceSeconds = 120;

    private boolean idle;
    private boolean meeting;

    public IdleDetectionService(
            UserActivityMonitor activityMonitor,
            int idleGraceMinutes
    ) {
        this.activityMonitor = activityMonitor;
        this.config = null;
        this.fallbackIdleGraceSeconds = Math.max(1, idleGraceMinutes) * 60;
        this.idle = false;
        this.meeting = false;
    }

    public IdleDetectionService(
            UserActivityMonitor activityMonitor,
            AgentConfig config
    ) {
        this.activityMonitor = activityMonitor;
        this.config = config;
        this.idle = false;
        this.meeting = false;
    }

    private int getEffectiveIdleGraceSeconds() {
        if (config != null) {
            return Math.max(1, config.getIdleGraceMinutes()) * 60;
        }
        return fallbackIdleGraceSeconds;
    }


    public AttendanceEventType checkIdleStatus() {

        // Do not mark the employee as idle while a meeting is active.
        if (meeting) {

            if (idle) {
                idle = false;
            }

            return null;
        }

        long inactiveSeconds =
                activityMonitor.getInactiveDurationSeconds();

        int graceSeconds = getEffectiveIdleGraceSeconds();

        if (!idle
                && inactiveSeconds >= graceSeconds) {

            idle = true;

            System.out.println(
                    "IDLE_STARTED - User inactive for "
                            + inactiveSeconds
                            + " seconds (threshold: "
                            + graceSeconds
                            + "s)"
            );

            return AttendanceEventType.IDLE_STARTED;
        }

        if (idle
                && inactiveSeconds < graceSeconds) {

            idle = false;

            System.out.println(
                    "IDLE_ENDED - User activity detected"
            );

            return AttendanceEventType.IDLE_ENDED;
        }

        return null;
    }

    public void setMeeting(boolean meeting) {
        this.meeting = meeting;
    }

    public boolean isMeeting() {
        return meeting;
    }

    public boolean isIdle() {
        return idle;
    }
}