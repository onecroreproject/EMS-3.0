package com.example.employee.controller;

import com.example.employee.model.Department;
import com.example.employee.model.Employee;
import com.example.employee.model.Team;
import com.example.employee.service.DepartmentService;
import com.example.employee.service.EmployeeService;
import com.example.employee.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Employee CRUD operations.
 * Designed for the new React frontend.
 */
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeWebController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final PasswordEncoder passwordEncoder;

    public EmployeeWebController(EmployeeService employeeService,
                                  DepartmentService departmentService,
                                  TeamService teamService,
                                  PasswordEncoder passwordEncoder) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.passwordEncoder = passwordEncoder;
    }

    // LIST ALL EMPLOYEES
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeService.findAll();
        employees.forEach(employeeService::enrichEmployeeWithDeptAndTeam);
        return ResponseEntity.ok(employees);
    }

    // GET SINGLE EMPLOYEE
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        Employee employee = employeeService.findById(id);
        if (employee == null) {
            return ResponseEntity.notFound().build();
        }
        employeeService.enrichEmployeeWithDeptAndTeam(employee);
        return ResponseEntity.ok(employee);
    }

    // CREATE OR UPDATE EMPLOYEE
    @PostMapping
    public ResponseEntity<Employee> saveEmployee(@RequestBody Employee employee) {
        if (employee.getPassword() != null && !employee.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        } else if (employee.getId() != null) {
            Employee existing = employeeService.findById(employee.getId());
            if (existing != null) {
                employee.setPassword(existing.getPassword());
            }
        }
        Employee saved = employeeService.save(employee);
        return ResponseEntity.ok(saved);
    }

    // DELETE EMPLOYEE
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEmployee(@PathVariable String id) {
        employeeService.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Employee deleted successfully");
        return ResponseEntity.ok(response);
    }

    // GET TEAMS BY DEPARTMENT (For Dropdowns)
    @GetMapping("/teams/by-department/{deptId}")
    public ResponseEntity<List<Team>> getTeamsByDepartment(@PathVariable String deptId) {
        List<Team> teams = teamService.getTeamsByDepartment(deptId)
                .stream()
                .map(dto -> {
                    Team t = new Team();
                    t.setId(dto.getId());
                    t.setName(dto.getName());
                    return t;
                })
                .toList();
        return ResponseEntity.ok(teams);
    }
}