package com.example.employee.dto;

public class LoginResponse {
    private String token;
    private String role;
    private String employeeCode;
    private String name;
    private String refreshToken;

    public LoginResponse(String token, String role, String employeeCode, String name, String refreshToken) {
        this.token = token;
        this.role = role;
        this.employeeCode = employeeCode;
        this.name = name;
        this.refreshToken = refreshToken;
    }

    public LoginResponse(String token, String role, String employeeCode, String name) {
        this.token = token;
        this.role = role;
        this.employeeCode = employeeCode;
        this.name = name;
        this.refreshToken = null;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
