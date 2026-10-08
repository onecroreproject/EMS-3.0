package com.example.employee.service;

import com.example.employee.dto.ActivityUsageSummaryDTO;
import com.example.employee.model.ApplicationActivityDocument;
import com.example.employee.repository.ApplicationActivityRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ApplicationActivityService {

    private final ApplicationActivityRepository repository;

    public ApplicationActivityService(
            ApplicationActivityRepository repository
    ) {
        this.repository = repository;
    }

    // ---------------------------------------------------------
    // Record application activity
    // ---------------------------------------------------------

    public ApplicationActivityDocument recordActivity(
            String employeeCode,
            String deviceId,
            String processName,
            int processId,
            String windowTitle,
            String url,
            String domain,
            Instant startedAt,
            Instant endedAt,
            long durationSeconds
    ) {

        ApplicationActivityDocument activity =
                new ApplicationActivityDocument();

        activity.setEmployeeCode(employeeCode);
        activity.setDeviceId(deviceId);
        activity.setProcessName(processName);
        activity.setProcessId(processId);
        activity.setWindowTitle(windowTitle);

        // NEW
        activity.setUrl(url);
        activity.setDomain(domain);

        activity.setStartedAt(startedAt);
        activity.setEndedAt(endedAt);
        activity.setDurationSeconds(durationSeconds);

        return repository.save(activity);
    }

    // ---------------------------------------------------------
    // Get employee activities
    // ---------------------------------------------------------

    public List<ApplicationActivityDocument> getEmployeeActivities(
            String employeeCode
    ) {

        return repository
                .findByEmployeeCodeOrderByStartedAtAsc(
                        employeeCode
                );
    }

    // ---------------------------------------------------------
    // Get employee activities between timestamps
    // ---------------------------------------------------------

    public List<ApplicationActivityDocument> getEmployeeActivitiesBetween(
            String employeeCode,
            Instant start,
            Instant end
    ) {

        return repository
                .findByEmployeeCodeAndStartedAtBetweenOrderByStartedAtAsc(
                        employeeCode,
                        start,
                        end
                );
    }

    // ---------------------------------------------------------
    // Get device activities
    // ---------------------------------------------------------

    public List<ApplicationActivityDocument> getDeviceActivities(
            String deviceId
    ) {

        return repository
                .findByDeviceIdOrderByStartedAtAsc(
                        deviceId
                );
    }



// =========================================================
// Top Applications
// =========================================================

    public List<ActivityUsageSummaryDTO> getTopApplications(
            String employeeCode,
            Instant start,
            Instant end
    ) {

        return repository.findTopApplications(
                employeeCode,
                start,
                end
        );
    }


// =========================================================
// Top Websites
// =========================================================

    public List<ActivityUsageSummaryDTO> getTopWebsites(
            String employeeCode,
            Instant start,
            Instant end
    ) {

        return repository.findTopWebsites(
                employeeCode,
                start,
                end
        );
    }





}