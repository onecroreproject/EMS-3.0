package com.example.employee.dto;

import java.time.LocalDateTime;

public class AttendanceDTO {
    private String employeeCode;
    private String employeeName; // <-- new
    private String email;
    private String departmentName;
    private String teamName;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    public String getEmployeeCode() {
        return employeeCode;
    }
    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }
    public String getEmployeeName() {
        return employeeName;
    }
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getDepartmentName() {
        return departmentName;
    }
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
    public String getTeamName() {
        return teamName;
    }
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    public LocalDateTime getClockIn() {
        return clockIn;
    }
    public void setClockIn(LocalDateTime clockIn) {
        this.clockIn = clockIn;
    }
    public LocalDateTime getClockOut() {
        return clockOut;
    }
    public void setClockOut(LocalDateTime clockOut) {
        this.clockOut = clockOut;
    }

    // Getters and setters
}
