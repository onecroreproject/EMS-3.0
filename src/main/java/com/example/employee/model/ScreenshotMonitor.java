package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "screenshot_monitor")
public class ScreenshotMonitor {

    @Id
    private String id;

    private String employeeId;
    private boolean active;
    private int intervalMinutes;

    public ScreenshotMonitor() {
    }

    public ScreenshotMonitor(String employeeId, boolean active, int intervalMinutes) {
        this.employeeId = employeeId;
        this.active = active;
        this.intervalMinutes = intervalMinutes;
    }

    // Getters and Setters
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(int intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }
}
