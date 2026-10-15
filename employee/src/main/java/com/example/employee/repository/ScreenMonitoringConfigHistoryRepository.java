package com.example.employee.repository;

import com.example.employee.model.ScreenMonitoringConfigHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenMonitoringConfigHistoryRepository extends MongoRepository<ScreenMonitoringConfigHistory, String> {
    List<ScreenMonitoringConfigHistory> findAllByOrderByTimestampDesc();
}
