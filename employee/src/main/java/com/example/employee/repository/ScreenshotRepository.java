package com.example.employee.repository;

import com.example.employee.model.Screenshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface ScreenshotRepository extends MongoRepository<Screenshot, String> {
    List<Screenshot> findByEmployeeId(String employeeId);
    List<Screenshot> findByEmployeeIdOrderByTimestampDesc(String employeeId);
    List<Screenshot> findTop6ByEmployeeIdOrderByTimestampDesc(String employeeId);
    List<Screenshot> findTop6ByOrderByTimestampDesc();
    Page<Screenshot> findByEmployeeId(String employeeId, Pageable pageable);
    Page<Screenshot> findByTimestampBetween(Date start, Date end, Pageable pageable);
    Page<Screenshot> findByEmployeeIdAndTimestampBetween(String employeeId, Date start, Date end, Pageable pageable);
}
