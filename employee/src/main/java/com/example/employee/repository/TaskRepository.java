package com.example.employee.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employee.model.Task;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByStatusAndAssignedTo(
            String status,
            String assignedTo);

    int countByStatusAndAssignedTo(
            String status,
            String assignedTo);

    // Count all tasks assigned to a specific employee
    int countByAssignedTo(String assignedTo);

    long countByStatus(String status);

    // Get all tasks assigned to an employee
    List<Task> findByAssignedTo(String assignedTo);

    // Get all tasks assigned by TL / Manager / Mentor
    List<Task> findByAssignedBy(String assignedBy);

    // Get employee tasks by status
    List<Task> findByAssignedToAndStatus(
            String assignedTo,
            String status);

    // Get tasks by type
    List<Task> findByTaskType(String taskType);

    // Get employee tasks by type
    List<Task> findByAssignedToAndTaskType(
            String assignedTo,
            String taskType);
}