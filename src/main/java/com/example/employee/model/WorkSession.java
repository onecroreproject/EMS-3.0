package com.example.employee.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "work_sessions")
public class WorkSession {

  @Id
    private String id;

    private String employeeCode;
    private String email;
    private String departmentId;
    private String departmentName;
    private String teamId;
    private String teamName;
    private String phone;
    private String designation;

    private String lastActiveTime;
private String lastIdleTime;
private LocalDateTime lastActivityTimestamp;  // Assuming timestamp is LocalDateTime


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


    @Field("date")
    private LocalDate date;

    private LocalDateTime clockIn;
    private LocalDateTime clockOut;

    private List<BreakPeriod> breaks = new ArrayList<>();

    private long totalBreakMinutes;
    
private boolean halfDayLeave;
private int permissionMinutes; // e.g., 120 for 2 hours

public boolean isHalfDayLeave() {
    return halfDayLeave;
}

public void setHalfDayLeave(boolean halfDayLeave) {
    this.halfDayLeave = halfDayLeave;
}

public int getPermissionMinutes() {
    return permissionMinutes;
}

public void setPermissionMinutes(int permissionMinutes) {
    this.permissionMinutes = permissionMinutes;
}

public long getTotalBreakMinutes() {
    return totalBreakMinutes;
}

public void setTotalBreakMinutes(long totalBreakMinutes) {
    this.totalBreakMinutes = totalBreakMinutes;
}

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public List<BreakPeriod> getBreaks() {
        return breaks;
    }

    public void setBreaks(List<BreakPeriod> breaks) {
        this.breaks = breaks;
    }

    // New method: total break time in minutes
    

    // New method: total worked minutes excluding breaks
    public long getTotalWorkedMinutes() {
        if (clockIn != null && clockOut != null) {
            long totalMinutes = Duration.between(clockIn, clockOut).toMinutes();
            long breakMinutes = getTotalBreakMinutes();
            return totalMinutes - breakMinutes;
        }
        return 0;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    // Inner class BreakPeriod
    public static class BreakPeriod {
        private LocalDateTime breakStart;
        private LocalDateTime breakEnd;

        public LocalDateTime getBreakStart() {
            return breakStart;
        }

        public void setBreakStart(LocalDateTime breakStart) {
            this.breakStart = breakStart;
        }

        public LocalDateTime getBreakEnd() {
            return breakEnd;
        }

        public void setBreakEnd(LocalDateTime breakEnd) {
            this.breakEnd = breakEnd;
        }
    }
}
