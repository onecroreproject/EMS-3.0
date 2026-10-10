package com.example.employee.dto;

import java.util.List;

import com.example.employee.model.Task;

public class EmployeeProductivityDTO {
    private String name;
    private int completedTasks;
    private int onTimeTasks;
    private int lateTasks;
    private int points;
    private List<TaskDTO> taskDetails; // Include full task details

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getOnTimeTasks() {
        return onTimeTasks;
    }

    public void setOnTimeTasks(int onTimeTasks) {
        this.onTimeTasks = onTimeTasks;
    }

    public int getLateTasks() {
        return lateTasks;
    }

    public void setLateTasks(int lateTasks) {
        this.lateTasks = lateTasks;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public List<TaskDTO> getTaskDetails() {
        return taskDetails;
    }

    public void setTaskDetails(List<TaskDTO> taskDetails) {
        this.taskDetails = taskDetails;
    }
}
