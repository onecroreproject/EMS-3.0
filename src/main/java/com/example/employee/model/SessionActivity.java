package com.example.employee.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "session_activity")
public class SessionActivity {
    @Id
    private String id;

    private String employeeCode;  // To associate with employee
    private String lastActiveTime;
    private String lastIdleTime;
    private LocalDateTime lastActivityTimestamp;

    // getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(String lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public String getLastIdleTime() {
        return lastIdleTime;
    }

    public void setLastIdleTime(String lastIdleTime) {
        this.lastIdleTime = lastIdleTime;
    }

    public LocalDateTime getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

    public void setLastActivityTimestamp(LocalDateTime lastActivityTimestamp) {
        this.lastActivityTimestamp = lastActivityTimestamp;
    }
}
