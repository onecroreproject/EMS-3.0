package com.example.employee.dto;

public class AgentLoginResponse {

    private boolean success;
    private String message;
    private String employeeId;
    private String employeeCode;
    private String employeeName;
    private String email;
    private String token;
    private String refreshToken;

    public AgentLoginResponse() {
    }

    public AgentLoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AgentLoginResponse(
            boolean success,
            String message,
            String employeeId,
            String employeeCode,
            String employeeName) {

        this.success = success;
        this.message = message;
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
    }

    public AgentLoginResponse(
            boolean success,
            String message,
            String employeeId,
            String employeeCode,
            String employeeName,
            String email,
            String token,
            String refreshToken) {

        this.success = success;
        this.message = message;
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.email = email;
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}