package org.example.attendance;

import java.time.Instant;

public class AgentState {

    private String employeeId;
    private String employeeCode;
    private String deviceId;

    /*
     * Session status:
     *
     * ACTIVE
     * ENDED
     * RECOVERY_REQUIRED
     */
    private String sessionStatus;

    /*
     * Existing AttendanceState value:
     *
     * WORKING
     * MEETING
     * IDLE
     * BREAK
     * LUNCH
     * OFFLINE
     * CLOCKED_OUT
     */
    private String currentState;

    private Instant workStartedAt;
    private Instant lastActivityAt;
    private Instant lastStateChangeAt;

    private Instant idleStartedAt;
    private Instant breakStartedAt;
    private Instant lunchStartedAt;

    private Instant lastHeartbeatAt;

    public AgentState() {
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public Instant getWorkStartedAt() {
        return workStartedAt;
    }

    public void setWorkStartedAt(Instant workStartedAt) {
        this.workStartedAt = workStartedAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Instant getLastStateChangeAt() {
        return lastStateChangeAt;
    }

    public void setLastStateChangeAt(Instant lastStateChangeAt) {
        this.lastStateChangeAt = lastStateChangeAt;
    }

    public Instant getIdleStartedAt() {
        return idleStartedAt;
    }

    public void setIdleStartedAt(Instant idleStartedAt) {
        this.idleStartedAt = idleStartedAt;
    }

    public Instant getBreakStartedAt() {
        return breakStartedAt;
    }

    public void setBreakStartedAt(Instant breakStartedAt) {
        this.breakStartedAt = breakStartedAt;
    }

    public Instant getLunchStartedAt() {
        return lunchStartedAt;
    }

    public void setLunchStartedAt(Instant lunchStartedAt) {
        this.lunchStartedAt = lunchStartedAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }
}