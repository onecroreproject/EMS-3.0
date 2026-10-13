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

import com.example.employee.model.ActiveSession;
import com.example.employee.repository.ActiveSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/agent")
public class AgentLoginController {

    private final EmployeeRepository employeeRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final ActiveSessionRepository activeSessionRepository;

    public AgentLoginController(EmployeeRepository employeeRepository, JwtTokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder, ActiveSessionRepository activeSessionRepository) {
        this.employeeRepository = employeeRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
        this.activeSessionRepository = activeSessionRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody AgentLoginRequest request, HttpServletRequest httpRequest) {

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
                employeeRepository.findFirstByEmail(request.getUsername());

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

        if (!request.isForce() && activeSessionRepository.findFirstByEmail(employee.getEmail()).isPresent()) {
            java.util.Map<String, Object> errorResp = new java.util.HashMap<>();
            errorResp.put("sessionActive", true);
            errorResp.put("message", "Already one session is active. If you login here, the previous login will expire.");
            return ResponseEntity.status(409).body(errorResp);
        }

        String token = jwtTokenUtil.generateToken(employee.getEmail(), "EMPLOYEE", employee.getEmployeeCode());
        String refreshToken = jwtTokenUtil.generateRefreshToken(employee.getEmail());

        activeSessionRepository.findFirstByEmail(employee.getEmail()).ifPresent(session -> {
            activeSessionRepository.delete(session);
        });
        String userAgent = httpRequest.getHeader("User-Agent");
        ActiveSession session = new ActiveSession(employee.getEmail(), token, refreshToken, userAgent, LocalDateTime.now());
        activeSessionRepository.save(session);

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
    public ResponseEntity<AgentLoginResponse> refresh(@RequestBody java.util.Map<String, String> request, HttpServletRequest httpRequest) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new AgentLoginResponse(false, "Refresh token is required"));
        }

        try {
            String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
            if (jwtTokenUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(401).body(new AgentLoginResponse(false, "Refresh token is expired"));
            }

            Optional<Employee> employeeOptional = employeeRepository.findFirstByEmail(username);
            if (employeeOptional.isEmpty()) {
                return ResponseEntity.status(401).body(new AgentLoginResponse(false, "Invalid user"));
            }

            Employee employee = employeeOptional.get();

            // Validate that the active session has not been replaced by another login
            Optional<ActiveSession> activeSessionOpt = activeSessionRepository.findFirstByEmail(employee.getEmail());
            if (activeSessionOpt.isEmpty() || (activeSessionOpt.get().getRefreshToken() != null && !activeSessionOpt.get().getRefreshToken().equals(refreshToken))) {
                return ResponseEntity.status(401).body(new AgentLoginResponse(false, "Session has been invalidated by another login"));
            }

            String newToken = jwtTokenUtil.generateToken(employee.getEmail(), "EMPLOYEE", employee.getEmployeeCode());
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(employee.getEmail());

            activeSessionOpt.ifPresent(activeSessionRepository::delete);
            String userAgent = httpRequest.getHeader("User-Agent");
            ActiveSession session = new ActiveSession(employee.getEmail(), newToken, newRefreshToken, userAgent, LocalDateTime.now());
            activeSessionRepository.save(session);

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
