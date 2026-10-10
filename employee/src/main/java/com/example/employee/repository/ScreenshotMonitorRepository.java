package com.example.employee.repository;

import com.example.employee.model.ScreenshotMonitor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScreenshotMonitorRepository extends MongoRepository<ScreenshotMonitor, String> {
    Optional<ScreenshotMonitor> findByEmployeeId(String employeeId);
    void deleteByEmployeeId(String employeeId);
    List<ScreenshotMonitor> findAll();  // Optional, already inherited from MongoRepository
}
