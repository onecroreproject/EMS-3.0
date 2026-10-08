package com.example.employee.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employee.model.AttendanceEventDocument;
import org.springframework.data.mongodb.repository.Query;

public interface AttendanceEventRepository
        extends MongoRepository<AttendanceEventDocument, String> {

    Optional<AttendanceEventDocument> findByEventId(String eventId);

    List<AttendanceEventDocument>
    findByEmployeeCodeOrderByTimestampAsc(
            String employeeCode
    );

    List<AttendanceEventDocument>
    findByEmployeeCodeAndTimestampBetweenOrderByTimestampAsc(
            String employeeCode,
            Instant start,
            Instant end
    );

    List<AttendanceEventDocument>
    findByDeviceIdOrderByTimestampAsc(
            String deviceId
    );

    Optional<AttendanceEventDocument>
    findTopByEmployeeCodeOrderByTimestampDesc(
            String employeeCode
    );

    List<AttendanceEventDocument>
    findByEmployeeCodeAndTimestampBetween(
            String employeeCode,
            Instant start,
            Instant end
    );

    // =====================================================
    // ADMIN - Attendance Events For One Calendar Day
    // =====================================================

    @Query("""
        {
            'employeeCode': ?0,
            'timestamp': {
                '$gte': ?1,
                '$lt': ?2
            }
        }
        """)
    List<AttendanceEventDocument> findAttendanceEventsForDay(
            String employeeCode,
            Instant startOfDay,
            Instant startOfNextDay,
            Sort sort
    );
}