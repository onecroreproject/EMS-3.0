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
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public AdminEmployeeController(EmployeeService employeeService, PasswordEncoder passwordEncoder, org.springframework.data.mongodb.core.MongoTemplate mongoTemplate) {
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<Employee>> getAllEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        
        if (search != null && !search.trim().isEmpty()) {
            org.springframework.data.mongodb.core.query.Criteria searchCriteria = new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                org.springframework.data.mongodb.core.query.Criteria.where("name").regex(search, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("email").regex(search, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("employeeCode").regex(search, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("phone").regex(search, "i"),
                org.springframework.data.mongodb.core.query.Criteria.where("alternativePhone").regex(search, "i")
            );
            query.addCriteria(searchCriteria);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("departmentId").is(departmentId));
        }
        if (teamId != null && !teamId.trim().isEmpty()) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("teamId").is(teamId));
        }
        if (status != null && !status.trim().isEmpty()) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("status").is(status));
        }
        
        long total = mongoTemplate.count(query, Employee.class);
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        query.with(pageable);
        
        java.util.List<Employee> employeesList = mongoTemplate.find(query, Employee.class);
        employeesList.forEach(employeeService::enrichEmployeeWithDeptAndTeam);
        
        org.springframework.data.domain.Page<Employee> employees = new org.springframework.data.domain.PageImpl<>(employeesList, pageable, total);
        
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        Employee emp = employeeService.findByAny(id);
        if (emp != null) {
            employeeService.enrichEmployeeWithDeptAndTeam(emp);
            return ResponseEntity.ok(emp);
        }
        return ResponseEntity.notFound().build();
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
        Employee existing = employeeService.findByAny(id);
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
        Employee existing = employeeService.findByAny(id);
        if (existing != null) {
            employeeService.deleteById(existing.getId());
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
