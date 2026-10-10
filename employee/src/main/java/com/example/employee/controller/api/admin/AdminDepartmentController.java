package com.example.employee.controller.api.admin;

import com.example.employee.model.Department;
import com.example.employee.repository.DepartmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/departments")
public class AdminDepartmentController {

    private final DepartmentRepository departmentRepository;

    public AdminDepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        return ResponseEntity.ok(departmentRepository.save(department));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable String id, @RequestBody Department departmentDetails) {
        Optional<Department> opt = departmentRepository.findById(id);
        if (opt.isPresent()) {
            Department dept = opt.get();
            dept.setName(departmentDetails.getName());
            dept.setDepartmentCode(departmentDetails.getDepartmentCode());
            dept.setDescription(departmentDetails.getDescription());
            dept.setDepartmentHead(departmentDetails.getDepartmentHead());
            dept.setStatus(departmentDetails.getStatus());
            return ResponseEntity.ok(departmentRepository.save(dept));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String id) {
        departmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
