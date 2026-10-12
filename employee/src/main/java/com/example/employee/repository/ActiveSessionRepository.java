package com.example.employee.repository;

import com.example.employee.model.ActiveSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ActiveSessionRepository extends MongoRepository<ActiveSession, String> {
    Optional<ActiveSession> findFirstByEmail(String email);
    Optional<ActiveSession> findByToken(String token);
    void deleteByEmail(String email);
}
