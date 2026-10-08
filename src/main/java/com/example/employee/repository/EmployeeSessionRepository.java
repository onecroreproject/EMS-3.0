package com.example.employee.repository;

import com.example.employee.model.EmployeeSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EmployeeSessionRepository
        extends MongoRepository<EmployeeSession, String> {

    Optional<EmployeeSession>
    findFirstByEmployeeIdAndStatus(
            String employeeId,
            String status
    );

    Optional<EmployeeSession>
    findByDeviceIdAndStatus(
            String deviceId,
            String status
    );
}
