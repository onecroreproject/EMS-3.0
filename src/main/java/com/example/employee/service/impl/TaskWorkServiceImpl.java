package com.example.employee.service.impl;

import com.example.employee.dto.TaskWorkResponse;
import com.example.employee.dto.TaskWorkStartRequest;
import com.example.employee.dto.TaskWorkStopRequest;
import com.example.employee.model.Employee;
import com.example.employee.model.EmployeeTaskWork;
import com.example.employee.model.Task;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.EmployeeTaskWorkRepository;
import com.example.employee.repository.TaskRepository;
import com.example.employee.service.TaskWorkService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskWorkServiceImpl implements TaskWorkService {

    private final EmployeeTaskWorkRepository employeeTaskWorkRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;

    public TaskWorkServiceImpl(
            EmployeeTaskWorkRepository employeeTaskWorkRepository,
            TaskRepository taskRepository,
            EmployeeRepository employeeRepository) {

        this.employeeTaskWorkRepository = employeeTaskWorkRepository;
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
    }


    // =========================================================
    // START TASK WORK
    // =========================================================

    @Override
    @Transactional
    public TaskWorkResponse startTaskWork(TaskWorkStartRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Task work start request cannot be null."
            );
        }

        if (isBlank(request.getTaskId())) {
            throw new IllegalArgumentException(
                    "Task ID is required."
            );
        }

        if (isBlank(request.getEmployeeId()) && isBlank(request.getEmployeeCode())) {
            throw new IllegalArgumentException(
                    "Employee ID or Employee code is required."
            );
        }

        if (isBlank(request.getDeviceId())) {
            throw new IllegalArgumentException(
                    "Device ID is required."
            );
        }

        // -----------------------------------------------------
        // 1. Find employee
        // -----------------------------------------------------

        Employee employee = null;
        if (!isBlank(request.getEmployeeId())) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElse(null);
        }
        if (employee == null && !isBlank(request.getEmployeeCode())) {
            employee = employeeRepository.findByEmployeeCode(request.getEmployeeCode())
                    .orElse(null);
        }
        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee not found."
            );
        }
        
        if (isBlank(request.getEmployeeId())) {
            request.setEmployeeId(employee.getId());
        }


        // -----------------------------------------------------
        // 2. Validate employee code
        // -----------------------------------------------------

        if (!request.getEmployeeCode()
                .equals(employee.getEmployeeCode())) {

            throw new IllegalArgumentException(
                    "Employee ID and employee code do not match."
            );
        }


        // -----------------------------------------------------
        // 3. Find task
        // -----------------------------------------------------

        Task task = taskRepository
                .findById(request.getTaskId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Task not found with ID: "
                                        + request.getTaskId()
                        )
                );


        // -----------------------------------------------------
        // 4. Validate task assignment
        // -----------------------------------------------------

        if (isBlank(task.getAssignedTo())) {

            throw new IllegalArgumentException(
                    "Task is not assigned to any employee."
            );
        }

        if (!task.getAssignedTo()
                .equalsIgnoreCase(employee.getName())) {

            throw new IllegalArgumentException(
                    "This task is not assigned to employee: "
                            + employee.getName()
            );
        }


        // -----------------------------------------------------
        // 5. Check existing running task
        // -----------------------------------------------------

        java.util.Optional<EmployeeTaskWork> existingWorkOpt = employeeTaskWorkRepository
                .findFirstByEmployeeIdAndStatusOrderByStartedAtDesc(
                        employee.getId(),
                        "RUNNING"
                );

        if (existingWorkOpt.isPresent()) {
            EmployeeTaskWork existingWork = existingWorkOpt.get();
            
            // Always auto-stop the old stuck task. We do not want to resume it
            // because the time elapsed might include hours where the agent was offline.
            // We set endedAt to startedAt to safely discard the stuck session's time.
            existingWork.setStatus("COMPLETED");
            if (existingWork.getStartedAt() != null) {
                existingWork.setEndedAt(existingWork.getStartedAt());
                existingWork.setDurationSeconds(0);
            } else {
                existingWork.setEndedAt(Instant.now());
                existingWork.setDurationSeconds(0);
            }
            employeeTaskWorkRepository.save(existingWork);
        }


        // -----------------------------------------------------
        // 6. Create new task work session
        // -----------------------------------------------------

        EmployeeTaskWork work = new EmployeeTaskWork();

        work.setTaskId(task.getId());
        work.setTaskTitle(task.getTitle());
        work.setAssignedBy(task.getAssignedBy());

        work.setEmployeeId(employee.getId());
        work.setEmployeeCode(employee.getEmployeeCode());

        work.setDeviceId(request.getDeviceId());

        work.setStartedAt(Instant.now());
        work.setEndedAt(null);

        work.setDurationSeconds(0);

        work.setStatus("RUNNING");


        // -----------------------------------------------------
        // 7. Save
        // -----------------------------------------------------

        EmployeeTaskWork savedWork =
                employeeTaskWorkRepository.save(work);


        System.out.println(
                "TASK WORK STARTED | "
                        + "Task: " + task.getTitle()
                        + " | Employee: " + employee.getName()
                        + " | Work ID: " + savedWork.getId()
        );


        return mapToResponse(savedWork);
    }


    // =========================================================
    // STOP TASK WORK
    // =========================================================

    @Override
    @Transactional
    public TaskWorkResponse stopTaskWork(
            String workId,
            TaskWorkStopRequest request) {

        if (isBlank(workId)) {

            throw new IllegalArgumentException(
                    "Work ID is required."
            );
        }

        if (request == null) {
            request = new TaskWorkStopRequest();
        }

        // -----------------------------------------------------
        // 1. Find work session
        // -----------------------------------------------------

        EmployeeTaskWork work =
                employeeTaskWorkRepository
                        .findById(workId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Task work session not found with ID: "
                                                + workId
                                )
                        );


        // -----------------------------------------------------
        // 2. Validate employee (if provided)
        // -----------------------------------------------------

        if (!isBlank(request.getEmployeeId()) && !work.getEmployeeId().equals(request.getEmployeeId())) {
            throw new IllegalArgumentException(
                    "This task work session does not belong "
                            + "to the specified employee."
            );
        }
        if (isBlank(request.getEmployeeId()) && !isBlank(request.getEmployeeCode()) 
            && !request.getEmployeeCode().equals(work.getEmployeeCode())) {
            throw new IllegalArgumentException(
                    "This task work session does not belong "
                            + "to the given employee code."
            );
        }


        // -----------------------------------------------------
        // 3. Check status
        // -----------------------------------------------------

        if (!"RUNNING".equalsIgnoreCase(work.getStatus())) {

            throw new IllegalStateException(
                    "Task work session is not currently running."
            );
        }


        // -----------------------------------------------------
        // 4. Set end time
        // -----------------------------------------------------

        Instant endedAt = Instant.now();

        work.setEndedAt(endedAt);


        // -----------------------------------------------------
        // 5. Calculate duration
        // -----------------------------------------------------

        Instant startedAt = work.getStartedAt();

        if (startedAt == null) {

            throw new IllegalStateException(
                    "Task work session has no start time."
            );
        }

        long durationSeconds =
                Duration.between(
                        startedAt,
                        endedAt
                ).getSeconds();


        if (durationSeconds < 0) {
            durationSeconds = 0;
        }


        work.setDurationSeconds(durationSeconds);


        // -----------------------------------------------------
        // 6. Update status
        // -----------------------------------------------------

        work.setStatus("STOPPED");


        // -----------------------------------------------------
        // 7. Save
        // -----------------------------------------------------

        EmployeeTaskWork savedWork =
                employeeTaskWorkRepository.save(work);


        System.out.println(
                "TASK WORK STOPPED | "
                        + "Task: " + savedWork.getTaskTitle()
                        + " | Duration: "
                        + savedWork.getDurationSeconds()
                        + " seconds"
        );


        return mapToResponse(savedWork);
    }


    // =========================================================
    // GET TASK WORK BY ID
    // =========================================================

    @Override
    public TaskWorkResponse getTaskWorkById(String workId) {

        if (isBlank(workId)) {

            throw new IllegalArgumentException(
                    "Work ID is required."
            );
        }

        EmployeeTaskWork work =
                employeeTaskWorkRepository
                        .findById(workId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Task work session not found with ID: "
                                                + workId
                                )
                        );

        return mapToResponse(work);
    }


    // =========================================================
    // GET ALL EMPLOYEE TASK WORK
    // =========================================================

    @Override
    public List<TaskWorkResponse> getEmployeeTaskWork(
            String employeeId) {

        if (isBlank(employeeId)) {

            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }

        return employeeTaskWorkRepository
                .findByEmployeeIdOrderByStartedAtDesc(employeeId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // =========================================================
    // GET TASK WORK BY TASK
    // =========================================================

    @Override
    public List<TaskWorkResponse> getTaskWorkByTask(
            String taskId) {

        if (isBlank(taskId)) {

            throw new IllegalArgumentException(
                    "Task ID is required."
            );
        }

        return employeeTaskWorkRepository
                .findByTaskIdOrderByStartedAtDesc(taskId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // =========================================================
    // GET EMPLOYEE TASK WORK BY TASK
    // =========================================================

    @Override
    public List<TaskWorkResponse> getEmployeeTaskWorkByTask(
            String employeeId,
            String taskId) {

        if (isBlank(employeeId)) {

            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }

        if (isBlank(taskId)) {

            throw new IllegalArgumentException(
                    "Task ID is required."
            );
        }

        return employeeTaskWorkRepository
                .findByEmployeeIdAndTaskIdOrderByStartedAtDesc(
                        employeeId,
                        taskId
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // =========================================================
    // GET CURRENTLY RUNNING TASK
    // =========================================================

    @Override
    public TaskWorkResponse getRunningTask(
            String employeeId) {

        if (isBlank(employeeId)) {

            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }

        return employeeTaskWorkRepository
                .findFirstByEmployeeIdAndStatusOrderByStartedAtDesc(
                        employeeId,
                        "RUNNING"
                )
                .map(this::mapToResponse)
                .orElse(null);
    }


    // =========================================================
    // ENTITY → RESPONSE DTO
    // =========================================================

    private TaskWorkResponse mapToResponse(
            EmployeeTaskWork work) {

        TaskWorkResponse response =
                new TaskWorkResponse();

        response.setId(work.getId());

        response.setTaskId(work.getTaskId());
        response.setTaskTitle(work.getTaskTitle());
        response.setAssignedBy(work.getAssignedBy());

        response.setEmployeeId(work.getEmployeeId());
        response.setEmployeeCode(work.getEmployeeCode());

        response.setDeviceId(work.getDeviceId());

        response.setStartedAt(work.getStartedAt());
        response.setEndedAt(work.getEndedAt());

        response.setDurationSeconds(
                work.getDurationSeconds()
        );

        response.setStatus(work.getStatus());

        return response;
    }


    // =========================================================
    // STRING VALIDATION
    // =========================================================

    private boolean isBlank(String value) {

        return value == null ||
                value.trim().isEmpty();
    }
}
