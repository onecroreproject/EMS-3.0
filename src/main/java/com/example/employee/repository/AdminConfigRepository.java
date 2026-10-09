package com.example.employee.repository;

import com.example.employee.model.AdminConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AdminConfigRepository extends MongoRepository<AdminConfig, String> {
    Optional<AdminConfig> findByEmail(String email);
}
