package com.example.employee.repository;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.employee.model.Email;

public interface EmailRepository extends MongoRepository<Email, String> {

    List<Email> findByToAndArchivedFalseAndSnoozedUntilIsNull(String to);

    List<Email> findByFrom(String from);

    List<Email> findByToAndStarredTrue(String to);

    List<Email> findByToAndArchivedTrue(String to);

    // Search in subject or body
    @Query("{ '$and': [ { '$or': [ { 'to': ?0 }, { 'from': ?0 }, { 'cc': ?0 } ] }, { '$or': [ { 'subject': { $regex: ?1, $options: 'i' } }, { 'body': { $regex: ?1, $options: 'i' } } ] } ] }")
    List<Email> findBySearchQuery(String email, String query);

    List<Email> findByFromOrToAndStarredTrue(String from, String to);

    long countByFrom(String from);

    @Query(
            value = "{ $or: [ { 'to': ?0 }, { 'cc': { $regex: ?0, $options: 'i' } } ] }",
            count = true
    )
    long countInboxByUserEmail(String email);

    @Query(
            value = "{ $or: [ { 'to': ?0 }, { 'cc': { $regex: ?0, $options: 'i' } } ], 'starred': true }",
            count = true
    )
    long countStarredByUserEmail(String email);

    @Query(
            value = "{ $or: [ { 'to': ?0 }, { 'cc': { $regex: ?0, $options: 'i' } } ], 'archived': true }",
            count = true
    )
    long countArchivedByUserEmail(String email);

}
