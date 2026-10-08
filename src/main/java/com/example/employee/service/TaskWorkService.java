package com.example.employee.service;

import com.example.employee.dto.TaskWorkResponse;
import com.example.employee.dto.TaskWorkStartRequest;
import com.example.employee.dto.TaskWorkStopRequest;

import java.util.List;

public interface TaskWorkService {

    /**
     * Start a new work session for a task.
     */
    TaskWorkResponse startTaskWork(TaskWorkStartRequest request);


    /**
     * Stop the currently running task work session.
     */
    TaskWorkResponse stopTaskWork(
            String workId,
            TaskWorkStopRequest request
    );


    /**
     * Get a particular task work session.
     */
    TaskWorkResponse getTaskWorkById(String workId);


    /**
     * Get all work sessions of an employee.
     */
    List<TaskWorkResponse> getEmployeeTaskWork(
            String employeeId
    );


    /**
     * Get all work sessions for a particular task.
     */
    List<TaskWorkResponse> getTaskWorkByTask(
            String taskId
    );


    /**
     * Get all work sessions of an employee for a particular task.
     */
    List<TaskWorkResponse> getEmployeeTaskWorkByTask(
            String employeeId,
            String taskId
    );


    /**
     * Get the currently running task of an employee.
     */
    TaskWorkResponse getRunningTask(
            String employeeId
    );
}
