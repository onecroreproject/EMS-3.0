package com.example.employee.repository;

import com.example.employee.dto.ActivityUsageSummaryDTO;
import com.example.employee.model.ApplicationActivityDocument;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface ApplicationActivityRepository
        extends MongoRepository<ApplicationActivityDocument, String> {

    // ---------------------------------------------------------
    // Get all employee activities
    // ---------------------------------------------------------

    List<ApplicationActivityDocument>
    findByEmployeeCodeOrderByStartedAtAsc(
            String employeeCode
    );

    // ---------------------------------------------------------
    // Get employee activities between timestamps
    // ---------------------------------------------------------

    List<ApplicationActivityDocument>
    findByEmployeeCodeAndStartedAtBetweenOrderByStartedAtAsc(
            String employeeCode,
            Instant start,
            Instant end
    );

    // ---------------------------------------------------------
    // Get device activities
    // ---------------------------------------------------------

    List<ApplicationActivityDocument>
    findByDeviceIdOrderByStartedAtAsc(
            String deviceId
    );
    
    ApplicationActivityDocument
    findFirstByDeviceIdOrderByStartedAtDesc(
            String deviceId
    );

    // =========================================================
    // TOP APPLICATIONS
    // =========================================================

    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'employeeCode': ?0, " +
                    "'startedAt': { '$gte': ?1, '$lt': ?2 }, " +
                    "'processName': { '$nin': [null, ''] } " +
                    "} }",

            "{ '$group': { " +
                    "'_id': '$processName', " +
                    "'totalDurationSeconds': { '$sum': '$durationSeconds' } " +
                    "} }",

            "{ '$project': { " +
                    "'_id': 0, " +
                    "'name': '$_id', " +
                    "'totalDurationSeconds': 1 " +
                    "} }",

            "{ '$sort': { " +
                    "'totalDurationSeconds': -1 " +
                    "} }",

            "{ '$limit': 5 }"
    })
    List<ActivityUsageSummaryDTO> findTopApplications(
            String employeeCode,
            Instant start,
            Instant end
    );


    // =========================================================
    // TOP WEBSITES
    // =========================================================

    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'employeeCode': ?0, " +
                    "'startedAt': { '$gte': ?1, '$lt': ?2 }, " +
                    "'domain': { '$nin': [null, ''] } " +
                    "} }",

            "{ '$group': { " +
                    "'_id': '$domain', " +
                    "'totalDurationSeconds': { '$sum': '$durationSeconds' } " +
                    "} }",

            "{ '$project': { " +
                    "'_id': 0, " +
                    "'name': '$_id', " +
                    "'totalDurationSeconds': 1 " +
                    "} }",

            "{ '$sort': { " +
                    "'totalDurationSeconds': -1 " +
                    "} }",

            "{ '$limit': 5 }"
    })
    List<ActivityUsageSummaryDTO> findTopWebsites(
            String employeeCode,
            Instant start,
            Instant end
    );
}