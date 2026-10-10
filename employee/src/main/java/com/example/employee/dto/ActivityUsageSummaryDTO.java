package com.example.employee.dto;

public class ActivityUsageSummaryDTO {

    private String name;
    private long totalDurationSeconds;

    public ActivityUsageSummaryDTO() {
    }

    public ActivityUsageSummaryDTO(
            String name,
            long totalDurationSeconds
    ) {
        this.name = name;
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(
            long totalDurationSeconds
    ) {
        this.totalDurationSeconds = totalDurationSeconds;
    }
}
