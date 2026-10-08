package com.example.employee.controller.api;

import com.example.employee.dto.EmployeeResponseDTO;
import com.example.employee.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
public class EmployeeApiController {

    private final EmployeeService employeeService;

    public EmployeeApiController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/by-name")
    public ResponseEntity<EmployeeResponseDTO> getEmployeeByName(@RequestParam String name) {
        Optional<EmployeeResponseDTO> empOpt = employeeService.getEmployeeWithNames(name);
        
        if (empOpt.isPresent()) {
            return ResponseEntity.ok(empOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
