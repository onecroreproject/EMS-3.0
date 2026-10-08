package com.example.employee.controller;

import com.example.employee.dto.TaskWorkResponse;
import com.example.employee.dto.TaskWorkStartRequest;
import com.example.employee.dto.TaskWorkStopRequest;
import com.example.employee.service.TaskWorkService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task-work")
public class TaskWorkController {

    private final TaskWorkService taskWorkService;

    public TaskWorkController(TaskWorkService taskWorkService) {
        this.taskWorkService = taskWorkService;
    }


    // =========================================================
    // START TASK WORK
    // =========================================================

    @PostMapping("/start")
    public ResponseEntity<TaskWorkResponse> startTaskWork(
            @RequestBody TaskWorkStartRequest request) {

        TaskWorkResponse response =
                taskWorkService.startTaskWork(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // STOP TASK WORK
    // =========================================================

    @PostMapping("/{workId}/stop")
    public ResponseEntity<TaskWorkResponse> stopTaskWork(
            @PathVariable String workId,
            @RequestBody TaskWorkStopRequest request) {

        TaskWorkResponse response =
                taskWorkService.stopTaskWork(
                        workId,
                        request
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET WORK SESSION BY ID
    // =========================================================

    @GetMapping("/{workId}")
    public ResponseEntity<TaskWorkResponse> getTaskWorkById(
            @PathVariable String workId) {

        TaskWorkResponse response =
                taskWorkService.getTaskWorkById(workId);

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET ALL WORK SESSIONS OF EMPLOYEE
    // =========================================================

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<TaskWorkResponse>> getEmployeeTaskWork(
            @PathVariable String employeeId) {

        List<TaskWorkResponse> response =
                taskWorkService.getEmployeeTaskWork(
                        employeeId
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET ALL WORK SESSIONS FOR TASK
    // =========================================================

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskWorkResponse>> getTaskWorkByTask(
            @PathVariable String taskId) {

        List<TaskWorkResponse> response =
                taskWorkService.getTaskWorkByTask(
                        taskId
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET EMPLOYEE WORK SESSIONS FOR A TASK
    // =========================================================

    @GetMapping("/employee/{employeeId}/task/{taskId}")
    public ResponseEntity<List<TaskWorkResponse>>
    getEmployeeTaskWorkByTask(
            @PathVariable String employeeId,
            @PathVariable String taskId) {

        List<TaskWorkResponse> response =
                taskWorkService.getEmployeeTaskWorkByTask(
                        employeeId,
                        taskId
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET CURRENTLY RUNNING TASK
    // =========================================================

    @GetMapping("/employee/{employeeId}/running")
    public ResponseEntity<TaskWorkResponse> getRunningTask(
            @PathVariable String employeeId) {

        TaskWorkResponse response =
                taskWorkService.getRunningTask(
                        employeeId
                );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }
}
