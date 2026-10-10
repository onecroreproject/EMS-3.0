package com.example.employee.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.employee.model.WorkSession;

public interface WorkSessionRepository extends MongoRepository<WorkSession, String> {

    List<WorkSession> findByEmployeeCodeOrderByDateDesc(String employeeCode);

    WorkSession findByEmployeeCodeAndDate(String employeeCode, LocalDate date);

    List<WorkSession> findAllByDate(LocalDate date);

    // Fetch all sessions (provided by JpaRepository by default)
    List<WorkSession> findAll();

    @Query("{ 'clockInTime': { $gt: ?0 } }")
    List<WorkSession> findLateArrivalsAfterNine(LocalDateTime nineAM);


    WorkSession findByEmailAndDate(String email, LocalDate date);


    @Query("SELECT COUNT(DISTINCT ws.employeeCode) FROM WorkSession ws WHERE ws.date = :today")
    long countDistinctByDate(@Param("today") LocalDate today);

       @Query("{ 'date' : ?0 }")
    List<WorkSession> findByDate(LocalDate date);

    // Count of late arrivals after 09:15
    @Query("{ 'date' : ?0, 'clockIn' : { $gt: ?1 } }")
    List<WorkSession> findLateArrivals(LocalDate date, LocalTime cutoff);

}
