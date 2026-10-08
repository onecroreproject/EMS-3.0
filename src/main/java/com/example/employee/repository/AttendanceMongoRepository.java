package com.example.employee.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.employee.model.Attendance;

public interface AttendanceMongoRepository extends MongoRepository<Attendance, String> {
    List<Attendance> findByDate(LocalDate date);
    List<Attendance> findByEmployeeCode(String employeeCode);

      Optional<Attendance> findByEmployeeCodeAndDate(String employeeCode, LocalDate date); 

      @Query("{ 'employeeCode': ?0, 'date': { $gte: ?1, $lte: ?2 } }")
List<Attendance> findByEmployeeCodeAndDateBetween(String employeeCode, LocalDate start, LocalDate end);



}
