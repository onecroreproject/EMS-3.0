package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "employee_activity")
public class EmployeeActivity {
    @Id
    private String id;
    private String employeeId;
    private long activeTimeSeconds;
    private long idleTimeSeconds;
    private String activeWindow;
    private LocalDateTime timestamp;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
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
