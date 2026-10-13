package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;
    private String title;
    private String description;
    private String category; // 'Security' | 'Employee' | 'Tasks' | 'System'
    private String iconType; // 'login' | 'offline' | 'task' | 'usage' | 'idle' | 'user' | 'system'
    private boolean isRead;
    private String link;
    private String targetRole = "ADMIN";
    private Date createdAt;

    public Notification() {
        this.createdAt = new Date();
        this.isRead = false;
        this.targetRole = "ADMIN";
    }

    public Notification(String title, String description, String category, String iconType, String link) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.iconType = iconType;
        this.link = link;
        this.isRead = false;
        this.targetRole = "ADMIN";
        this.createdAt = new Date();
    }

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIconType() {
        return iconType;
    }

    public void setIconType(String iconType) {
        this.iconType = iconType;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
