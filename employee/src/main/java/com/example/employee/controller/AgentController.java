package com.example.employee.controller;

import com.example.employee.model.Device;
import com.example.employee.repository.DeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.example.employee.security.JwtTokenUtil;
import com.example.employee.model.Employee;
import com.example.employee.repository.EmployeeRepository;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final DeviceRepository deviceRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final EmployeeRepository employeeRepository;

    public AgentController(DeviceRepository deviceRepository, JwtTokenUtil jwtTokenUtil, EmployeeRepository employeeRepository) {
        this.deviceRepository = deviceRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(
            @RequestParam String deviceId) {

        Map<String, Object> response = new HashMap<>();

        Device device = deviceRepository
                .findByDeviceId(deviceId)
                .orElse(null);

        if (device == null) {

            response.put("success", false);
            response.put("deviceId", deviceId);
            response.put("status", "NOT_REGISTERED");
            response.put("message", "Device is not registered");

            return ResponseEntity.status(404).body(response);
        }

        Instant now = Instant.now();

        if ("INVALIDATED".equalsIgnoreCase(device.getStatus())) {
            response.put("success", true);
            response.put("deviceId", deviceId);
            response.put("status", "OFFLINE");
            response.put("serverTime", now);
            return ResponseEntity.ok(response);
        }

        device.setStatus("ONLINE");
        device.setLastSeen(now);

        deviceRepository.save(device);

        response.put("success", true);
        response.put("deviceId", deviceId);
        response.put("status", device.getStatus());
        response.put("serverTime", now);

        return ResponseEntity.ok(response);
    }


}