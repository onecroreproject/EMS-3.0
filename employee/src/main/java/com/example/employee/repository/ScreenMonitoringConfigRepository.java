package com.example.employee.repository;

import com.example.employee.model.ScreenMonitoringConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScreenMonitoringConfigRepository extends MongoRepository<ScreenMonitoringConfig, String> {
    Optional<ScreenMonitoringConfig> findFirstByOrderByIdAsc();
}
