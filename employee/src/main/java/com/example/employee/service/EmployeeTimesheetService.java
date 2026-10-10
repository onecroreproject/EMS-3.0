package com.example.employee.service;

import com.example.employee.dto.AttendanceTimelineItem;
import com.example.employee.model.Department;
import com.example.employee.model.Employee;
import com.example.employee.model.Team;
import com.example.employee.repository.DepartmentRepository;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeTimesheetService {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final EmployeeRepository employeeRepository;

    private final TeamRepository teamRepository;

    private final DepartmentRepository departmentRepository;

    private final AdminAttendanceTimelineService
            attendanceTimelineService;


    public EmployeeTimesheetService(

            EmployeeRepository employeeRepository,

            TeamRepository teamRepository,

            DepartmentRepository departmentRepository,

            AdminAttendanceTimelineService
                    attendanceTimelineService) {

        this.employeeRepository =
                employeeRepository;

        this.teamRepository =
                teamRepository;

        this.departmentRepository =
                departmentRepository;

        this.attendanceTimelineService =
                attendanceTimelineService;
    }


    // =========================================================
    // MAIN TIMESHEET
    // =========================================================

    public Map<String, Object> buildTimesheet(

            String employeeCode,

            String period,

            LocalDate anchorDate) {

        period =
                period == null
                        ? "DAY"
                        : period.toUpperCase();

        DateRange range =
                calculateRange(
                        period,
                        anchorDate
                );


        Employee employee =
                employeeRepository
                        .findAll()
                        .stream()
                        .filter(e ->
                                employeeCode.equals(
                                        e.getEmployeeCode()
                                )
                        )
                        .findFirst()
                        .orElse(null);


        String employeeName =
                employee != null
                        ? employee.getName()
                        : employeeCode;


        String departmentName =
                getDepartmentName(employee);


        List<Map<String, Object>> dailyRows =
                new ArrayList<>();


        Map<String, Long> overall =
                emptyTotals();


        LocalDate current =
                range.start;


        while (!current.isAfter(range.end)) {

            List<AttendanceTimelineItem> timeline =
                    attendanceTimelineService.getTimeline(
                            employeeCode,
                            current
                    );


            Map<String, Long> dayTotals =
                    calculateTotals(timeline);


            addTotals(
                    overall,
                    dayTotals
            );


            Map<String, Object> row =
                    new LinkedHashMap<>();


            row.put(
                    "date",
                    current.toString()
            );


            row.put(
                    "label",
                    current.format(
                            DISPLAY_DATE
                    )
            );


            row.put(
                    "dayName",
                    current.getDayOfWeek()
                            .toString()
                            .substring(0, 3)
            );


            row.put(
                    "workSeconds",
                    dayTotals.get("workSeconds")
            );


            row.put(
                    "idleSeconds",
                    dayTotals.get("idleSeconds")
            );


            row.put(
                    "lunchSeconds",
                    dayTotals.get("lunchSeconds")
            );


            row.put(
                    "breakSeconds",
                    dayTotals.get("breakSeconds")
            );


            long totalSeconds =
                    calculateTotal(
                            dayTotals
                    );


            row.put(
                    "totalSeconds",
                    totalSeconds
            );


            row.put(
                    "efficiency",
                    calculateEfficiency(
                            dayTotals
                    )
            );


            dailyRows.add(row);


            current =
                    current.plusDays(1);
        }


        List<Map<String, Object>> displayRows;


        if ("YEAR".equals(period)) {

            displayRows =
                    buildYearRows(
                            dailyRows
                    );

        } else {

            displayRows =
                    dailyRows;
        }


        Map<String, Object> response =
                new LinkedHashMap<>();


        response.put(
                "employeeCode",
                employeeCode
        );


        response.put(
                "employeeName",
                employeeName
        );


        response.put(
                "departmentName",
                departmentName
        );


        response.put(
                "period",
                period
        );


        response.put(
                "startDate",
                range.start.toString()
        );


        response.put(
                "endDate",
                range.end.toString()
        );


        response.put(
                "startDateLabel",
                range.start.format(
                        DISPLAY_DATE
                )
        );


        response.put(
                "endDateLabel",
                range.end.format(
                        DISPLAY_DATE
                )
        );


        response.put(
                "summary",
                createSummary(
                        overall
                )
        );


        response.put(
                "rows",
                displayRows
        );


        /*
         * Day view gets hourly breakdown.
         */
        if ("DAY".equals(period)) {

            List<AttendanceTimelineItem> timeline =
                    attendanceTimelineService.getTimeline(
                            employeeCode,
                            anchorDate
                    );


            response.put(
                    "hourly",
                    buildHourlyRows(
                            timeline
                    )
            );
        }


        return response;
    }


    // =========================================================
    // RANGE
    // =========================================================

    private DateRange calculateRange(

            String period,

            LocalDate date) {

        LocalDate start;

        LocalDate end;


        switch (period) {

            case "WEEK":

                start =
                        date.with(
                                DayOfWeek.MONDAY
                        );

                end =
                        start.plusDays(6);

                break;


            case "MONTH":

                start =
                        date.withDayOfMonth(1);

                end =
                        date.withDayOfMonth(
                                date.lengthOfMonth()
                        );

                break;


            case "YEAR":

                start =
                        date.withDayOfYear(1);

                end =
                        date.withDayOfYear(
                                date.lengthOfYear()
                        );

                break;


            case "DAY":

            default:

                start = date;

                end = date;

                break;
        }


        return new DateRange(
                start,
                end
        );
    }


    // =========================================================
    // DAILY TOTALS
    // =========================================================

    private Map<String, Long> calculateTotals(

            List<AttendanceTimelineItem> timeline) {

        Map<String, Long> totals =
                emptyTotals();


        for (AttendanceTimelineItem item :
                timeline) {

            long seconds =
                    item.getDurationSeconds();


            String type =
                    item.getType();


            if ("WORKING".equalsIgnoreCase(type)) {

                totals.merge(
                        "workSeconds",
                        seconds,
                        Long::sum
                );

            } else if ("IDLE".equalsIgnoreCase(type)) {

                totals.merge(
                        "idleSeconds",
                        seconds,
                        Long::sum
                );

            } else if ("LUNCH".equalsIgnoreCase(type)) {

                totals.merge(
                        "lunchSeconds",
                        seconds,
                        Long::sum
                );

            } else if ("BREAK".equalsIgnoreCase(type)) {

                totals.merge(
                        "breakSeconds",
                        seconds,
                        Long::sum
                );
            }
        }


        return totals;
    }


    private Map<String, Long> emptyTotals() {

        Map<String, Long> totals =
                new LinkedHashMap<>();

        totals.put(
                "workSeconds",
                0L
        );

        totals.put(
                "idleSeconds",
                0L
        );

        totals.put(
                "lunchSeconds",
                0L
        );

        totals.put(
                "breakSeconds",
                0L
        );

        return totals;
    }


    private void addTotals(

            Map<String, Long> target,

            Map<String, Long> source) {

        target.merge(
                "workSeconds",
                source.get("workSeconds"),
                Long::sum
        );

        target.merge(
                "idleSeconds",
                source.get("idleSeconds"),
                Long::sum
        );

        target.merge(
                "lunchSeconds",
                source.get("lunchSeconds"),
                Long::sum
        );

        target.merge(
                "breakSeconds",
                source.get("breakSeconds"),
                Long::sum
        );
    }


    private long calculateTotal(
            Map<String, Long> totals) {

        return totals.get("workSeconds")
                + totals.get("idleSeconds")
                + totals.get("lunchSeconds")
                + totals.get("breakSeconds");
    }


    /*
     * Efficiency is the percentage of recorded
     * employee time spent in WORKING state.
     */
    private long calculateEfficiency(
            Map<String, Long> totals) {

        long total =
                calculateTotal(totals);


        if (total <= 0) {
            return 0;
        }


        return Math.round(
                (
                        totals.get("workSeconds")
                                * 100.0
                ) / total
        );
    }


    // =========================================================
    // SUMMARY
    // =========================================================

    private Map<String, Object> createSummary(

            Map<String, Long> totals) {

        Map<String, Object> summary =
                new LinkedHashMap<>();


        long work =
                totals.get("workSeconds");

        long idle =
                totals.get("idleSeconds");

        long lunch =
                totals.get("lunchSeconds");

        long breakTime =
                totals.get("breakSeconds");


        long total =
                work
                        + idle
                        + lunch
                        + breakTime;


        summary.put(
                "workSeconds",
                work
        );

        summary.put(
                "idleSeconds",
                idle
        );

        summary.put(
                "lunchSeconds",
                lunch
        );

        summary.put(
                "breakSeconds",
                breakTime
        );

        summary.put(
                "effectiveWorkSeconds",
                work
        );

        summary.put(
                "totalSeconds",
                total
        );

        summary.put(
                "efficiency",
                calculateEfficiency(totals)
        );


        return summary;
    }


    // =========================================================
    // YEARLY ROWS
    // =========================================================

    private List<Map<String, Object>> buildYearRows(

            List<Map<String, Object>> dailyRows) {

        Map<YearMonth, Map<String, Long>>
                grouped =
                new LinkedHashMap<>();


        for (Map<String, Object> row :
                dailyRows) {

            LocalDate date =
                    LocalDate.parse(
                            (String) row.get("date")
                    );


            YearMonth month =
                    YearMonth.from(date);


            Map<String, Long> totals =
                    grouped.computeIfAbsent(
                            month,
                            key -> emptyTotals()
                    );


            totals.merge(
                    "workSeconds",
                    ((Number) row.get(
                            "workSeconds"
                    )).longValue(),
                    Long::sum
            );


            totals.merge(
                    "idleSeconds",
                    ((Number) row.get(
                            "idleSeconds"
                    )).longValue(),
                    Long::sum
            );


            totals.merge(
                    "lunchSeconds",
                    ((Number) row.get(
                            "lunchSeconds"
                    )).longValue(),
                    Long::sum
            );


            totals.merge(
                    "breakSeconds",
                    ((Number) row.get(
                            "breakSeconds"
                    )).longValue(),
                    Long::sum
            );
        }


        List<Map<String, Object>> rows =
                new ArrayList<>();


        for (Map.Entry<YearMonth,
                Map<String, Long>> entry :
                grouped.entrySet()) {

            Map<String, Long> totals =
                    entry.getValue();


            Map<String, Object> row =
                    new LinkedHashMap<>();


            row.put(
                    "date",
                    entry.getKey().toString()
            );


            row.put(
                    "label",
                    entry.getKey()
                            .getMonth()
                            .toString()
                            .substring(0, 1)
                            + entry.getKey()
                            .getMonth()
                            .toString()
                            .substring(1)
                            .toLowerCase()
                            + " "
                            + entry.getKey()
                            .getYear()
            );


            row.put(
                    "workSeconds",
                    totals.get("workSeconds")
            );


            row.put(
                    "idleSeconds",
                    totals.get("idleSeconds")
            );


            row.put(
                    "lunchSeconds",
                    totals.get("lunchSeconds")
            );


            row.put(
                    "breakSeconds",
                    totals.get("breakSeconds")
            );


            row.put(
                    "totalSeconds",
                    calculateTotal(totals)
            );


            row.put(
                    "efficiency",
                    calculateEfficiency(totals)
            );


            rows.add(row);
        }


        return rows;
    }


    // =========================================================
    // HOURLY DAY VIEW
    // =========================================================

    private List<Map<String, Object>>
    buildHourlyRows(

            List<AttendanceTimelineItem> timeline) {

        Map<Integer, Map<String, Long>>
                buckets =
                new LinkedHashMap<>();


        for (AttendanceTimelineItem item :
                timeline) {

            if (item.getStartedAt() == null
                    || item.getEndedAt() == null) {

                continue;
            }


            Instant cursor =
                    item.getStartedAt();

            Instant end =
                    item.getEndedAt();


            while (cursor.isBefore(end)) {

                java.time.ZonedDateTime zoned =
                        cursor.atZone(
                                INDIA_ZONE
                        );


                int hour =
                        zoned.getHour();


                Instant hourEnd =
                        zoned.toLocalDate()
                                .atTime(
                                        hour,
                                        0
                                )
                                .plusHours(1)
                                .atZone(
                                        INDIA_ZONE
                                )
                                .toInstant();


                Instant segmentEnd =
                        end.isBefore(hourEnd)
                                ? end
                                : hourEnd;


                long seconds =
                        Duration.between(
                                cursor,
                                segmentEnd
                        ).getSeconds();


                Map<String, Long> bucket =
                        buckets.computeIfAbsent(
                                hour,
                                key -> emptyTotals()
                        );


                String type =
                        item.getType();


                if ("WORKING".equalsIgnoreCase(type)) {

                    bucket.merge(
                            "workSeconds",
                            seconds,
                            Long::sum
                    );

                } else if ("IDLE".equalsIgnoreCase(type)) {

                    bucket.merge(
                            "idleSeconds",
                            seconds,
                            Long::sum
                    );

                } else if ("LUNCH".equalsIgnoreCase(type)) {

                    bucket.merge(
                            "lunchSeconds",
                            seconds,
                            Long::sum
                    );

                } else if ("BREAK".equalsIgnoreCase(type)) {

                    bucket.merge(
                            "breakSeconds",
                            seconds,
                            Long::sum
                    );
                }


                cursor =
                        segmentEnd;
            }
        }


        List<Map<String, Object>> rows =
                new ArrayList<>();


        for (Map.Entry<Integer,
                Map<String, Long>> entry :
                buckets.entrySet()) {

            int hour =
                    entry.getKey();


            Map<String, Long> totals =
                    entry.getValue();


            Map<String, Object> row =
                    new LinkedHashMap<>();


            row.put(
                    "hour",
                    String.format(
                            "%02d:00",
                            hour
                    )
            );


            row.put(
                    "workSeconds",
                    totals.get("workSeconds")
            );


            row.put(
                    "idleSeconds",
                    totals.get("idleSeconds")
            );


            row.put(
                    "lunchSeconds",
                    totals.get("lunchSeconds")
            );


            row.put(
                    "breakSeconds",
                    totals.get("breakSeconds")
            );


            row.put(
                    "totalSeconds",
                    calculateTotal(totals)
            );


            rows.add(row);
        }


        return rows;
    }


    // =========================================================
    // EMPLOYEE DEPARTMENT
    // =========================================================

    private String getDepartmentName(
            Employee employee) {

        if (employee == null
                || employee.getTeamId() == null) {

            return "—";
        }


        Team team =
                teamRepository
                        .findById(
                                employee.getTeamId()
                        )
                        .orElse(null);


        if (team == null
                || team.getDepartmentId() == null) {

            return "—";
        }


        Department department =
                departmentRepository
                        .findById(
                                team.getDepartmentId()
                        )
                        .orElse(null);


        return department != null
                ? department.getName()
                : "—";
    }


    // =========================================================
    // DATE RANGE
    // =========================================================

    private static class DateRange {

        private final LocalDate start;

        private final LocalDate end;


        private DateRange(
                LocalDate start,
                LocalDate end) {

            this.start = start;

            this.end = end;
        }
    }
}
