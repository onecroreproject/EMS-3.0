
package com.example.employee.dto;

import java.time.LocalDateTime;

public class EmployeeActivityDto {
    private String employeeId;
    private long activeTimeSeconds;
    private long idleTimeSeconds;
    private String activeWindow;
    private LocalDateTime timestamp = LocalDateTime.now();
    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    public long getActiveTimeSeconds() {
        return activeTimeSeconds;
    }
    public void setActiveTimeSeconds(long activeTimeSeconds) {
        this.activeTimeSeconds = activeTimeSeconds;
    }
    public long getIdleTimeSeconds() {
        return idleTimeSeconds;
    }
    public void setIdleTimeSeconds(long idleTimeSeconds) {
        this.idleTimeSeconds = idleTimeSeconds;
    }
    public String getActiveWindow() {
        return activeWindow;
    }
    public void setActiveWindow(String activeWindow) {
        this.activeWindow = activeWindow;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Getters and Setters
}
