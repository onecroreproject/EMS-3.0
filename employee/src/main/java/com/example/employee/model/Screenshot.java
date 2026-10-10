package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "screenshots")
public class Screenshot {
    @Id
    private String id;

    private String employeeId;
    private byte[] imageData;
    private Date timestamp;

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
}
