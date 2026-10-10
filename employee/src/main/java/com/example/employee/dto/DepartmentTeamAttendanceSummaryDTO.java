package com.example.employee.dto;

public class DepartmentTeamAttendanceSummaryDTO {

    private String departmentId;
    private String departmentName;
    private String teamId;
    private String teamName;
    private long totalEmployees;
    private long present;
    private long absent;
    private long lateArrivals;

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

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getPresent() {
        return present;
    }

    public void setPresent(long present) {
        this.present = present;
    }

    public long getAbsent() {
        return absent;
    }

    public void setAbsent(long absent) {
        this.absent = absent;
    }

    public long getLateArrivals() {
        return lateArrivals;
    }

    public void setLateArrivals(long lateArrivals) {
        this.lateArrivals = lateArrivals;
    }
}
