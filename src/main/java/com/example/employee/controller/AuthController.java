package com.example.employee.controller;

import com.example.employee.dto.LoginRequest;
import com.example.employee.dto.LoginResponse;
import com.example.employee.model.Employee;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password.hash}")
    private String adminPasswordHash;

    public AuthController(EmployeeRepository employeeRepository, JwtTokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Admin Login
        if (adminEmail.equalsIgnoreCase(loginRequest.getEmail())) {
            if (passwordEncoder.matches(loginRequest.getPassword(), adminPasswordHash)) {
                String jwt = jwtTokenUtil.generateToken(adminEmail, "ADMIN", null);
                String refresh = jwtTokenUtil.generateRefreshToken(adminEmail);
                return ResponseEntity.ok(new LoginResponse(jwt, "ADMIN", null, "System Admin", refresh));
            }
        }

        // Employee Login
        Optional<Employee> empOpt = employeeRepository.findByEmail(loginRequest.getEmail());
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            if (passwordEncoder.matches(loginRequest.getPassword(), emp.getPassword())) {
                String jwt = jwtTokenUtil.generateToken(emp.getEmail(), "EMPLOYEE", emp.getEmployeeCode());
                String refresh = jwtTokenUtil.generateRefreshToken(emp.getEmail());
                return ResponseEntity.ok(new LoginResponse(jwt, "EMPLOYEE", emp.getEmployeeCode(), emp.getName(), refresh));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
    }

    // Explicit endpoint for agent (just in case they need a distinct path, but logic is same)
    @PostMapping("/agent/login")
    public ResponseEntity<?> authenticateAgent(@RequestBody LoginRequest loginRequest) {
        return authenticateUser(loginRequest);
    }
}
