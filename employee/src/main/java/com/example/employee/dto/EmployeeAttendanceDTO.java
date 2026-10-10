package com.example.employee.dto;


public class EmployeeAttendanceDTO {
    private String employeeCode;
    private String name;
    private String designation;
    private String email;
    private String status;
    private String clockInTime; // formatted like "09:32 AM"
private String clockOutTime;
private String workedHours; // e.g., "7h 45m"


    
    public String getClockOutTime() {
    return clockOutTime;
}
public void setClockOutTime(String clockOutTime) {
    this.clockOutTime = clockOutTime;
}
    public String getEmployeeCode() {
        return employeeCode;
    }
    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDesignation() {
        return designation;
    }
    public void setDesignation(String designation) {
        this.designation = designation;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getClockInTime() {
        return clockInTime;
    }
    public void setClockInTime(String clockInTime) {
        this.clockInTime = clockInTime;
    }

    // Getters and setters

    public String getWorkedHours() {
        return workedHours;
    }

    public void setWorkedHours(String workedHours) {
        this.workedHours = workedHours;
    }
}
