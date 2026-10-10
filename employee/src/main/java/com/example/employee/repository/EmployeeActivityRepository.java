package com.example.employee.repository;

import com.example.employee.model.EmployeeActivity;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmployeeActivityRepository extends MongoRepository<EmployeeActivity, String> {

    
}
