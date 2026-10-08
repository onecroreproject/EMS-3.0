package com.example.employee.controller.api.admin;

import com.example.employee.model.Employee;
import com.example.employee.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {

    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;

    public AdminEmployeeController(EmployeeService employeeService, PasswordEncoder passwordEncoder) {
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<Employee>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(employeeService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        Employee emp = employeeService.findById(id);
        return emp != null ? ResponseEntity.ok(emp) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@jakarta.validation.Valid @RequestBody Employee employee) {
        // Hash password before saving
        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        }
        return ResponseEntity.ok(employeeService.save(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable String id, @jakarta.validation.Valid @RequestBody Employee employeeDetails) {
        Employee existing = employeeService.findById(id);
        if (existing != null) {
            existing.setName(employeeDetails.getName());
            existing.setEmail(employeeDetails.getEmail());
            existing.setPhone(employeeDetails.getPhone());
            existing.setDepartmentId(employeeDetails.getDepartmentId());
            existing.setTeamId(employeeDetails.getTeamId());
            existing.setDesignation(employeeDetails.getDesignation());
            existing.setStatus(employeeDetails.getStatus());
            
            // Only update password if provided
            if (employeeDetails.getPassword() != null && !employeeDetails.getPassword().isEmpty()) {
                existing.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
            }
            
            return ResponseEntity.ok(employeeService.save(existing));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String id) {
        employeeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
