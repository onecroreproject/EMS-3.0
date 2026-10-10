package com.example.employee.repository;

import com.example.employee.model.AgentUpdate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AgentUpdateRepository
        extends MongoRepository<AgentUpdate, String> {

    List<AgentUpdate> findByPlatformAndActiveTrue(
            String platform
    );

    Optional<AgentUpdate> findByVersionAndPlatform(
            String version,
            String platform
    );

    List<AgentUpdate> findByPlatformOrderByCreatedAtDesc(
            String platform
    );
}