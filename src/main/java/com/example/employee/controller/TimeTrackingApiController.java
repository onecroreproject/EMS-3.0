package com.example.employee.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.employee.model.EmployeeLocation;
import com.example.employee.repository.EmployeeLocationRepository;
import com.example.employee.service.WorkSessionService;

@RestController
@RequestMapping("/api/employee/time")
public class TimeTrackingApiController {

    @Autowired
    private EmployeeLocationRepository locationRepository;

    private final WorkSessionService workSessionService;

    public TimeTrackingApiController(WorkSessionService workSessionService) {
        this.workSessionService = workSessionService;
    }

    @PostMapping("/clock-in")
    public ResponseEntity<String> clockIn(
            @RequestParam String employeeCode,
            @RequestParam String email,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) String address
    ) {
        System.out.println("Clock-in API hit with:");
        System.out.println("Employee: " + employeeCode + ", Email: " + email);
        System.out.println("Lat: " + lat + ", Lon: " + lon + ", Address: " + address);

        try {
            workSessionService.clockIn(employeeCode, email);

            if (lat != null && lon != null) {
                EmployeeLocation loc = new EmployeeLocation();
                loc.setEmployeeCode(employeeCode);
                loc.setEmail(email);
                loc.setLatitude(lat);
                loc.setLongitude(lon);
                loc.setAddress(address);
                loc.setTimestamp(LocalDateTime.now());
                locationRepository.save(loc);
            }

            return ResponseEntity.ok("Clocked in successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    @PostMapping("/clock-out")
    public ResponseEntity<String> clockOut(@RequestParam String employeeCode) {
        workSessionService.clockOut(employeeCode);
        return ResponseEntity.ok("Clocked out successfully");
    }

    @PostMapping("/break/start")
    public ResponseEntity<String> startBreak(@RequestParam String employeeCode) {
        workSessionService.startBreak(employeeCode);
        return ResponseEntity.ok("Break started");
    }

    @PostMapping("/break/end")
    public ResponseEntity<String> endBreak(@RequestParam String employeeCode) {
        workSessionService.endBreak(employeeCode);
        return ResponseEntity.ok("Break ended");
    }
}
