package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "screenshots")
@CompoundIndexes({
    @CompoundIndex(name = "emp_time_idx", def = "{'employeeId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "empcode_time_idx", def = "{'employeeCode': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "dev_time_idx", def = "{'deviceId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "time_idx", def = "{'timestamp': -1}")
})
public class Screenshot {
    @Id
    private String id;

    @Indexed
    private String employeeId;
    private byte[] imageData;
    @Indexed
    private Date timestamp;

    // Enhanced metadata
    private String employeeName;
    private String employeeCode;
    private String deviceId;
    private String hostname;
    private String status; // Working, Idle, Break
    private String appName;
    private String windowTitle;
    private String activityDuration;
    private String fileName;

    public Screenshot() {}

    public Screenshot(String employeeId, byte[] imageData, Date timestamp) {
        this.employeeId = employeeId;
        this.imageData = imageData;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getWindowTitle() { return windowTitle; }
    public void setWindowTitle(String windowTitle) { this.windowTitle = windowTitle; }

    public String getActivityDuration() { return activityDuration; }
    public void setActivityDuration(String activityDuration) { this.activityDuration = activityDuration; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
