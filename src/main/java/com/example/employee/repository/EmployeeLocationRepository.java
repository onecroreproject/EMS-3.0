package com.example.employee.repository;

import com.example.employee.model.EmployeeLocation;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeLocationRepository extends MongoRepository<EmployeeLocation, String> {

     List<EmployeeLocation> findAllByOrderByTimestampDesc();
}
