package org.example.attendance;

import org.example.activity.UserActivityMonitor;

public class IdleDetectionService {

    private final UserActivityMonitor activityMonitor;
    private final int idleGraceSeconds;

    private boolean idle;
    private boolean meeting;

    public IdleDetectionService(
            UserActivityMonitor activityMonitor,
            int idleGraceMinutes
    ) {
        this.activityMonitor = activityMonitor;
        this.idleGraceSeconds =
                idleGraceMinutes * 60;

        this.idle = false;
        this.meeting = false;
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

        if (!idle
                && inactiveSeconds >= idleGraceSeconds) {

            idle = true;

            System.out.println(
                    "IDLE_STARTED - User inactive for "
                            + inactiveSeconds
                            + " seconds"
            );

            return AttendanceEventType.IDLE_STARTED;
        }

        if (idle
                && inactiveSeconds < idleGraceSeconds) {

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