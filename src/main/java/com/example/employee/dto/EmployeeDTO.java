package com.example.employee.dto;

public class EmployeeDTO {
    private String id;
    private String employeeCode;
    private String name;
    private String email;
    private String status;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    private String departmentName;
    private String teamName;

    // constructors
    public EmployeeDTO() {}

    public EmployeeDTO(String id, String employeeCode, String name, String email, String status, String departmentName, String teamName) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.status = status;
        this.departmentName = departmentName;
        this.teamName = teamName;
    }

    // getters and setters

    // ... generate all getters and setters here
}
