package com.example.employee.dto;

import java.time.Instant;

public class DeviceResponseDTO {

    private String deviceId;
    private String employeeId;
    private String employeeCode;
    private String hostname;
    private String operatingSystem;
    private String agentVersion;
    private String status;
    private String activity;
    private Instant registeredAt;
    private Instant lastSeen;

    public DeviceResponseDTO() {
    }

    public DeviceResponseDTO(
            String deviceId,
            String employeeId,
            String employeeCode,
            String hostname,
            String operatingSystem,
            String agentVersion,
            String status,
            String activity,
            Instant registeredAt,
            Instant lastSeen) {

        this.deviceId = deviceId;
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.hostname = hostname;
        this.operatingSystem = operatingSystem;
        this.agentVersion = agentVersion;
        this.status = status;
        this.activity = activity;
        this.registeredAt = registeredAt;
        this.lastSeen = lastSeen;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
}