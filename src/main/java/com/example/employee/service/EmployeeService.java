package com.example.employee.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.employee.dto.EmployeeDTO;
import com.example.employee.dto.EmployeeResponseDTO;
import com.example.employee.model.Department;
import com.example.employee.model.Employee;
import com.example.employee.model.Team;
import com.example.employee.repository.DepartmentRepository;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.TeamRepository;

@Service
public class EmployeeService {

    @Autowired
    private DepartmentRepository departmentRepo;

    @Autowired
    private TeamRepository teamRepo;

    private final EmployeeRepository employeeRepo;

    public EmployeeService(EmployeeRepository employeeRepo) {
        this.employeeRepo = employeeRepo;
    }

    public List<Employee> findAll() {
        return employeeRepo.findAll();
    }

    public org.springframework.data.domain.Page<Employee> findAll(org.springframework.data.domain.Pageable pageable) {
        return employeeRepo.findAll(pageable);
    }

    public Employee findById(String id) {
        return employeeRepo.findById(id).orElse(null);
    }

    public Employee save(Employee employee) {
        return employeeRepo.save(employee);
    }

    public void deleteById(String id) {
        employeeRepo.deleteById(id);
    }

    public Employee findByAny(String input) {
        return employeeRepo
                .findByIdOrEmailOrPhoneOrEmployeeCode(input, input, input, input)
                .orElse(null);
    }

    public Employee findByEmail(String email) {
        return employeeRepo.findByEmail(email).orElse(null);
    }

    public Employee enrichEmployeeWithDeptAndTeam(Employee emp) {
        if (emp.getDepartmentId() != null) {
            String deptName = departmentRepo.findById(emp.getDepartmentId())
                    .map(Department::getName)
                    .orElse("");
            emp.setDepartmentName(deptName);
        }

        if (emp.getTeamId() != null) {
            String teamName = teamRepo.findById(emp.getTeamId())
                    .map(Team::getName)
                    .orElse("");
            emp.setTeamName(teamName);
        }

        return emp;
    }

    public Employee findByEmployeeCode(String employeeCode) {
        return employeeRepo.findByEmployeeCode(employeeCode).orElse(null);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepo.findAll();
    }

    public Optional<EmployeeResponseDTO> getEmployeeWithNames(String name) {
        Optional<Employee> empOpt = employeeRepo.findByName(name);

        if (empOpt.isEmpty()) {
            return Optional.empty();
        }

        Employee emp = empOpt.get();

        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setEmployeeCode(emp.getEmployeeCode());
        dto.setEmail(emp.getEmail());
        dto.setDesignation(emp.getDesignation());
        dto.setStatus(emp.getStatus());
       
        departmentRepo.findById(emp.getDepartmentId())
                .ifPresent(dept -> dto.setDepartmentName(dept.getName()));

        teamRepo.findById(emp.getTeamId())
                .ifPresent(team -> dto.setTeamName(team.getName()));

        return Optional.of(dto);
    }
 public List<EmployeeDTO> getEmployeesByTeam(String teamId) {
    List<Employee> employees = employeeRepo.findByTeamId(teamId);
    return employees.stream()
        .map(emp -> new EmployeeDTO(
            emp.getId(),
            emp.getEmployeeCode(),
            emp.getName(),
            emp.getEmail(),
            emp.getStatus(),
            emp.getDepartmentName(),  // or however you set this
            emp.getTeamName()
        ))
        .collect(Collectors.toList());
}



}
