package com.example.employee.service;

import com.example.employee.dto.EmployeeActivityDto;
import com.example.employee.model.EmployeeActivity;
import com.example.employee.repository.EmployeeActivityRepository;
import org.springframework.stereotype.Service;

@Service
public class EmployeeActivityService {

    private final EmployeeActivityRepository repository;

    public EmployeeActivityService(EmployeeActivityRepository repository) {
        this.repository = repository;
    }

    public void saveActivity(EmployeeActivityDto dto) {
        EmployeeActivity activity = new EmployeeActivity();
        activity.setEmployeeId(dto.getEmployeeId());
        activity.setActiveTimeSeconds(dto.getActiveTimeSeconds());
        activity.setIdleTimeSeconds(dto.getIdleTimeSeconds());
        activity.setActiveWindow(dto.getActiveWindow());
        activity.setTimestamp(dto.getTimestamp());

        repository.save(activity);
    }
}
