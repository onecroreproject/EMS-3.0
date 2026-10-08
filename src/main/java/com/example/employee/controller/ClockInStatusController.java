package com.example.employee.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.employee.model.WorkSession;
import com.example.employee.repository.WorkSessionRepository;

@RestController
@RequestMapping("/api/employee/clockin")
public class
ClockInStatusController {

    @Autowired
    private WorkSessionRepository workSessionRepository;

    /**
     * Check if the employee is currently clocked in (based on today's WorkSession using email)
     */
    @GetMapping("/status/{email}")
    public boolean isEmployeeClockedIn(@PathVariable String email) {
        LocalDate today = LocalDate.now();

        WorkSession session = workSessionRepository.findByEmailAndDate(email, today);

        if (session != null && session.getClockIn() != null && session.getClockOut() == null) {
            return true;
        }

        return false;
    }
}
