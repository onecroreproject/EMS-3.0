package com.example.employee.repository;

import com.example.employee.model.EmployeeTaskWork;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeTaskWorkRepository
        extends MongoRepository<EmployeeTaskWork, String> {

    // Check whether the employee already has a running task
    Optional<EmployeeTaskWork>
    findFirstByEmployeeIdAndStatusOrderByStartedAtDesc(
            String employeeId,
            String status
    );

    // Get all work sessions of an employee
    List<EmployeeTaskWork>
    findByEmployeeIdOrderByStartedAtDesc(
            String employeeId
    );

    // Get all work sessions for a particular task
    List<EmployeeTaskWork>
    findByTaskIdOrderByStartedAtDesc(
            String taskId
    );

    // Get all sessions of a particular employee for a particular task
    List<EmployeeTaskWork>
    findByEmployeeIdAndTaskIdOrderByStartedAtDesc(
            String employeeId,
            String taskId
    );
}
