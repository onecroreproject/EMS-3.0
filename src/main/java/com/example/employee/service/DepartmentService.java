package com.example.employee.service;

import com.example.employee.model.Department;
import java.util.List;
import java.util.Optional;

public interface DepartmentService {
    void saveDepartment(Department department);
    List<Department> getAllDepartments();
    Optional<Department> getDepartmentById(String id);
    void deleteDepartmentById(String id);
}
