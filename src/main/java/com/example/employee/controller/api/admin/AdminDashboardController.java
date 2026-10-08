package com.example.employee.controller.api.admin;

import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.DepartmentRepository;
import com.example.employee.repository.TeamRepository;
import com.example.employee.repository.TaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;

    public AdminDashboardController(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository, TeamRepository teamRepository, TaskRepository taskRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalEmployees", employeeRepository.count());
        stats.put("totalDepartments", departmentRepository.count());
        stats.put("totalTeams", teamRepository.count());
        stats.put("totalTasks", taskRepository.count());
        
        // TODO: add real-time presence data from WorkSessionRepository
        
        return ResponseEntity.ok(stats);
    }
}
