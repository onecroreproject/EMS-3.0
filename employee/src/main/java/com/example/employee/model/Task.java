package com.example.employee.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    // =========================
    // Task Information
    // =========================

    private String title;
    private String description;


    // =========================
    // Assignment Information
    // =========================

    // Employee / Trainee who receives the task
    @org.springframework.data.mongodb.core.index.Indexed
    private String assignedTo;

    // TL / Mentor / Manager who assigns the task
    @org.springframework.data.mongodb.core.index.Indexed
    private String assignedBy;


    // =========================
    // Task Classification
    // =========================

    // PROJECT / TRAINING / DEVELOPMENT / SUPPORT / MENTORING
    private String taskType;


    // =========================
    // Date Information
    // =========================

    @org.springframework.data.mongodb.core.index.Indexed
    private LocalDate startDate;
    @org.springframework.data.mongodb.core.index.Indexed
    private String endDate;
    private LocalDate completedDate;


    // =========================
    // Task Management
    // =========================

    // ASSIGNED / ACCEPTED / IN_PROGRESS / COMPLETED
    @org.springframework.data.mongodb.core.index.Indexed
    private String status;

    // LOW / MEDIUM / HIGH
    @org.springframework.data.mongodb.core.index.Indexed
    private String priority;


    // =========================
    // Employee/Trainee Remarks
    // =========================

    private String remarks;


    // =========================
    // Getters and Setters
    // =========================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }


    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }


    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }


    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }


    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }


    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }


    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}