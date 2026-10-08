package com.example.employee.repository;

import com.example.employee.model.Screenshot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ScreenshotRepository extends MongoRepository<Screenshot, String> {
    List<Screenshot> findByEmployeeId(String employeeId);
}
