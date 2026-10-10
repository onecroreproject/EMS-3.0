package com.example.employee.controller;

import com.example.employee.dto.AgentLoginRequest;
import com.example.employee.dto.AgentLoginResponse;
import com.example.employee.model.Employee;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.security.JwtTokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/agent")
public class AgentLoginController {

    private final EmployeeRepository employeeRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    public AgentLoginController(EmployeeRepository employeeRepository, JwtTokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<AgentLoginResponse> login(
            @RequestBody AgentLoginRequest request) {

        if (request.getUsername() == null ||
                request.getUsername().trim().isEmpty() ||
                request.getPassword() == null ||
                request.getPassword().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(new AgentLoginResponse(
                            false,
                            "Username and password are required"
                    ));
        }

        Optional<Employee> employeeOptional =
                employeeRepository.findByEmail(request.getUsername());

        if (employeeOptional.isEmpty()) {

            return ResponseEntity.status(401)
                    .body(new AgentLoginResponse(
                            false,
                            "Invalid username or password"
                    ));
        }

        Employee employee = employeeOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            return ResponseEntity.status(401)
                    .body(new AgentLoginResponse(
                            false,
                            "Invalid username or password"
                    ));
        }

        String token = jwtTokenUtil.generateToken(employee.getEmail(), "EMPLOYEE", employee.getEmployeeCode());
        String refreshToken = jwtTokenUtil.generateRefreshToken(employee.getEmail());

        return ResponseEntity.ok(
                new AgentLoginResponse(
                        true,
                        "Agent login successful",
                        employee.getId(),
                        employee.getEmployeeCode(),
                        employee.getName(),
                        employee.getEmail(),
                        token,
                        refreshToken
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AgentLoginResponse> refresh(@RequestBody java.util.Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AgentLoginResponse(false, "Refresh token is required"));
        }

        try {
            String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
            if (jwtTokenUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(401).body(new AgentLoginResponse(false, "Refresh token is expired"));
            }

            Optional<Employee> employeeOptional = employeeRepository.findByEmail(username);
            if (employeeOptional.isEmpty()) {
                return ResponseEntity.status(401).body(new AgentLoginResponse(false, "Invalid user"));
            }

            Employee employee = employeeOptional.get();
            String newToken = jwtTokenUtil.generateToken(employee.getEmail(), "EMPLOYEE", employee.getEmployeeCode());
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(employee.getEmail());

            return ResponseEntity.ok(
                    new AgentLoginResponse(
                            true,
                            "Token refreshed successfully",
                            employee.getId(),
                            employee.getEmployeeCode(),
                            employee.getName(),
                            employee.getEmail(),
                            newToken,
                            newRefreshToken
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new AgentLoginResponse(false, "Invalid refresh token"));
        }
    }
}
