package com.example.employee.repository;

import com.example.employee.model.EmployeeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeStatusRepository extends MongoRepository<EmployeeStatus, String> {
}
