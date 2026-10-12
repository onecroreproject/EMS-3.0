package com.example.employee.controller;

import com.example.employee.model.Device;
import com.example.employee.repository.DeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/agent")
public class DeviceController {

    private final DeviceRepository deviceRepository;

    public DeviceController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    private String extractClientIp(HttpServletRequest request, String providedIp) {
        if (providedIp != null && !providedIp.trim().isEmpty() && !"127.0.0.1".equals(providedIp)) {
            return providedIp;
        }
        if (request != null) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isEmpty()) {
                return xfHeader.split(",")[0].trim();
            }
            String remoteAddr = request.getRemoteAddr();
            if (remoteAddr != null && !remoteAddr.isEmpty()) {
                return "0:0:0:0:0:0:0:1".equals(remoteAddr) ? "127.0.0.1" : remoteAddr;
            }
        }
        return providedIp != null ? providedIp : "";
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @RequestBody Device device,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        if (device.getDeviceId() == null ||
                device.getDeviceId().trim().isEmpty()) {

            response.put("success", false);
            response.put("message", "deviceId is required");

            return ResponseEntity.badRequest().body(response);
        }

        Device existingDevice =
                deviceRepository.findByDeviceId(device.getDeviceId())
                        .orElse(null);

        if (device.getEmployeeCode() != null && !device.getEmployeeCode().trim().isEmpty()) {
            java.util.List<Device> otherDevices = deviceRepository.findByEmployeeCode(device.getEmployeeCode());
            for (Device other : otherDevices) {
                if (!other.getDeviceId().equals(device.getDeviceId())) {
                    other.setStatus("INVALIDATED");
                    deviceRepository.save(other);
                }
            }
        }

        String clientIp = extractClientIp(request, device.getIpAddress());

        if (existingDevice != null) {

            existingDevice.setEmployeeId(device.getEmployeeId());
            existingDevice.setEmployeeCode(device.getEmployeeCode());
            existingDevice.setHostname(device.getHostname());
            existingDevice.setOperatingSystem(device.getOperatingSystem());
            if (device.getAgentVersion() != null && !device.getAgentVersion().isBlank()) {
                existingDevice.setAgentVersion(device.getAgentVersion());
            }
            if (clientIp != null && !clientIp.isBlank()) {
                existingDevice.setIpAddress(clientIp);
            }
            if (device.getLocation() != null && !device.getLocation().isBlank()) {
                existingDevice.setLocation(device.getLocation());
            }
            existingDevice.setStatus("ONLINE");
            existingDevice.setLastSeen(Instant.now());

            deviceRepository.save(existingDevice);

            response.put("success", true);
            response.put("message", "Device already registered");
            response.put("deviceId", existingDevice.getDeviceId());
            response.put("status", existingDevice.getStatus());

            return ResponseEntity.ok(response);
        }

        device.setStatus("ONLINE");
        if (clientIp != null && !clientIp.isBlank()) {
            device.setIpAddress(clientIp);
        }
        device.setRegisteredAt(Instant.now());
        device.setLastSeen(Instant.now());

        Device savedDevice = deviceRepository.save(device);

        response.put("success", true);
        response.put("message", "Device registered successfully");
        response.put("deviceId", savedDevice.getDeviceId());
        response.put("status", savedDevice.getStatus());

        return ResponseEntity.ok(response);
    }
}