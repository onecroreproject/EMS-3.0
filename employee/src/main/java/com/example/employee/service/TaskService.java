package com.example.employee.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.employee.model.Task;
import com.example.employee.repository.TaskRepository;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    // Create / Update Task
    public void saveTask(Task task) {
        taskRepository.save(task);
    }

    // Get all tasks
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // Get task by ID
    public Task getTaskById(String id) {
        return taskRepository.findById(id).orElse(null);
    }

    // Delete task
    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }

    // Save task
    public void save(Task task) {
        taskRepository.save(task);
    }

    // Get completed tasks for employee
    public List<Task> getCompletedTasksByAssignedTo(String assignedTo) {
        return taskRepository.findByStatusAndAssignedTo(
                "Completed",
                assignedTo
        );
    }

    // Count tasks by status and employee
    public int countTasksByStatusAndAssignedTo(
            String status,
            String assignedTo) {

        return taskRepository.countByStatusAndAssignedTo(
                status,
                assignedTo
        );
    }

    // Count total tasks for employee
    public int countTasksByAssignedTo(String assignedTo) {
        return taskRepository.countByAssignedTo(assignedTo);
    }

    // -------------------------------------------------
    // Corporate Task Management
    // -------------------------------------------------

    // Get tasks assigned to employee
    public List<Task> getTasksByAssignedTo(String assignedTo) {
        return taskRepository.findByAssignedTo(assignedTo);
    }

    // Get tasks assigned by TL / Manager / Mentor
    public List<Task> getTasksByAssignedBy(String assignedBy) {
        return taskRepository.findByAssignedBy(assignedBy);
    }

    // Get employee tasks by status
    public List<Task> getTasksByAssignedToAndStatus(
            String assignedTo,
            String status) {

        return taskRepository.findByAssignedToAndStatus(
                assignedTo,
                status
        );
    }

    // Get tasks by type
    public List<Task> getTasksByTaskType(String taskType) {
        return taskRepository.findByTaskType(taskType);
    }

    // Get employee tasks by type
    public List<Task> getTasksByAssignedToAndTaskType(
            String assignedTo,
            String taskType) {

        return taskRepository.findByAssignedToAndTaskType(
                assignedTo,
                taskType
        );
    }
}