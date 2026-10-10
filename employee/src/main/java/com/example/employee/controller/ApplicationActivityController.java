package com.example.employee.controller;

import com.example.employee.dto.ApplicationActivityRequest;
import com.example.employee.model.ApplicationActivityDocument;
import com.example.employee.service.ApplicationActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/agent/activity")
public class ApplicationActivityController {

    private final ApplicationActivityService activityService;

    public ApplicationActivityController(
            ApplicationActivityService activityService
    ) {
        this.activityService = activityService;
    }

    // ---------------------------------------------------------
    // Record application activity
    // ---------------------------------------------------------

    @PostMapping
    public ResponseEntity<ApplicationActivityDocument> recordActivity(
            @RequestBody ApplicationActivityRequest request
    ) {

        ApplicationActivityDocument savedActivity =
                activityService.recordActivity(
                        request.getEmployeeCode(),
                        request.getDeviceId(),
                        request.getProcessName(),
                        request.getProcessId(),
                        request.getWindowTitle(),

                        // NEW
                        request.getUrl(),
                        request.getDomain(),

                        request.getStartedAt(),
                        request.getEndedAt(),
                        request.getDurationSeconds()
                );

        return ResponseEntity.ok(savedActivity);
    }

    // ---------------------------------------------------------
    // Get employee activities
    // ---------------------------------------------------------

    @GetMapping("/employee/{employeeCode}")
    public ResponseEntity<List<ApplicationActivityDocument>>
    getEmployeeActivities(
            @PathVariable String employeeCode
    ) {

        return ResponseEntity.ok(
                activityService.getEmployeeActivities(
                        employeeCode
                )
        );
    }

    // ---------------------------------------------------------
    // Get employee activities between timestamps
    // ---------------------------------------------------------

    @GetMapping("/employee/{employeeCode}/between")
    public ResponseEntity<List<ApplicationActivityDocument>>
    getEmployeeActivitiesBetween(
            @PathVariable String employeeCode,
            @RequestParam String start,
            @RequestParam String end
    ) {

        Instant startTime =
                Instant.parse(start);

        Instant endTime =
                Instant.parse(end);

        return ResponseEntity.ok(
                activityService.getEmployeeActivitiesBetween(
                        employeeCode,
                        startTime,
                        endTime
                )
        );
    }

    // ---------------------------------------------------------
    // Get device activities
    // ---------------------------------------------------------

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<ApplicationActivityDocument>>
    getDeviceActivities(
            @PathVariable String deviceId
    ) {

        return ResponseEntity.ok(
                activityService.getDeviceActivities(
                        deviceId
                )
        );
    }
}