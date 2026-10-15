package com.example.employee.controller;

import com.example.employee.model.Screenshot;
import com.example.employee.repository.ScreenshotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;

@RestController
@RequestMapping("/api/agent/screenshot")
public class AgentScreenshotController {

    private final ScreenshotRepository screenshotRepository;

    public AgentScreenshotController(ScreenshotRepository screenshotRepository) {
        this.screenshotRepository = screenshotRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadScreenshot(
            @RequestParam("file") MultipartFile file,
            @RequestParam("employeeId") String employeeId) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Uploaded file is empty");
            }
            byte[] imageData = file.getBytes();
            Screenshot screenshot = new Screenshot(employeeId, imageData, new Date());
            screenshotRepository.save(screenshot);
            return ResponseEntity.ok("Screenshot saved successfully for employee: " + employeeId);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
}
