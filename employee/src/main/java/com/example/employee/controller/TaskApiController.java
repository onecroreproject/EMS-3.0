package com.example.employee.controller;

import com.example.employee.model.Task;
import com.example.employee.repository.TaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.employee.model.Employee;
import com.example.employee.repository.EmployeeRepository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;

    public TaskApiController(TaskRepository taskRepository, EmployeeRepository employeeRepository) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(
            @RequestParam(required = false) String employeeCode,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String empName) {

        // If no filter requested, return all tasks (used by admin/reports)
        if ((employeeCode == null || employeeCode.isBlank())
                && (employeeId == null || employeeId.isBlank())
                && (empName == null || empName.isBlank())) {
            return ResponseEntity.ok(taskRepository.findAll());
        }

        // Collect all possible identifiers for this employee (Name, EmployeeCode, Mongo ID)
        Set<String> searchIdentifiers = new LinkedHashSet<>();
        if (employeeCode != null && !employeeCode.isBlank()) {
            searchIdentifiers.add(employeeCode.trim());
        }
        if (employeeId != null && !employeeId.isBlank()) {
            searchIdentifiers.add(employeeId.trim());
        }
        if (empName != null && !empName.isBlank()) {
            searchIdentifiers.add(empName.trim());
        }

        // If employeeCode or employeeId was provided, resolve the Employee record to also match by name/id
        if (employeeCode != null && !employeeCode.isBlank()) {
            employeeRepository.findFirstByEmployeeCode(employeeCode.trim()).ifPresent(emp -> {
                if (emp.getName() != null && !emp.getName().isBlank()) searchIdentifiers.add(emp.getName().trim());
                if (emp.getId() != null && !emp.getId().isBlank()) searchIdentifiers.add(emp.getId().trim());
            });
        }
        if (employeeId != null && !employeeId.isBlank()) {
            employeeRepository.findFirstById(employeeId.trim()).ifPresent(emp -> {
                if (emp.getName() != null && !emp.getName().isBlank()) searchIdentifiers.add(emp.getName().trim());
                if (emp.getEmployeeCode() != null && !emp.getEmployeeCode().isBlank()) searchIdentifiers.add(emp.getEmployeeCode().trim());
            });
        }

        List<Task> all = taskRepository.findAll();
        List<Task> filtered = new ArrayList<>();

        for (Task t : all) {
            String assigned = t.getAssignedTo();
            if (assigned == null || assigned.isBlank()) {
                // If a task is unassigned or for everyone, include it
                continue;
            }
            assigned = assigned.trim();
            for (String id : searchIdentifiers) {
                if (assigned.equalsIgnoreCase(id) || assigned.toLowerCase().contains(id.toLowerCase())) {
                    filtered.add(t);
                    break;
                }
            }
        }

        return ResponseEntity.ok(filtered);
    }
}
