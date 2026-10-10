package com.example.employee.repository;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.employee.model.ActivityLog;

@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    List<ActivityLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

   List<ActivityLog> findByEmployeeEmailAndDate(String employeeEmail, LocalDate date);



}
