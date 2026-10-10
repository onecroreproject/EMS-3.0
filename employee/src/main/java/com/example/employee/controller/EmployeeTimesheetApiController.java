package com.example.employee.controller;

import com.example.employee.service.EmployeeTimesheetService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/timesheet")
public class EmployeeTimesheetApiController {

    private final EmployeeTimesheetService employeeTimesheetService;

    public EmployeeTimesheetApiController(
            EmployeeTimesheetService employeeTimesheetService) {

        this.employeeTimesheetService =
                employeeTimesheetService;
    }

    @GetMapping
    public Map<String, Object> getTimesheet(

            @RequestParam String employeeCode,

            @RequestParam(defaultValue = "DAY")
            String period,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return employeeTimesheetService.buildTimesheet(
                employeeCode,
                period,
                date
        );
    }
}