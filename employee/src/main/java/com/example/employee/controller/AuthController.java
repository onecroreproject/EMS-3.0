package com.example.employee.controller;

import com.example.employee.dto.LoginRequest;
import com.example.employee.dto.LoginResponse;
import com.example.employee.dto.MfaVerifyRequest;
import com.example.employee.model.ActiveSession;
import com.example.employee.repository.ActiveSessionRepository;
import com.example.employee.model.AdminConfig;
import com.example.employee.model.Employee;
import com.example.employee.repository.AdminConfigRepository;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.security.JwtTokenUtil;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import com.example.employee.service.NotificationService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final AdminConfigRepository adminConfigRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthenticator gAuth;
    private final NotificationService notificationService;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password.hash}")
    private String adminPasswordHash;

    public AuthController(EmployeeRepository employeeRepository, AdminConfigRepository adminConfigRepository, ActiveSessionRepository activeSessionRepository, JwtTokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder, NotificationService notificationService) {
        this.employeeRepository = employeeRepository;
        this.adminConfigRepository = adminConfigRepository;
        this.activeSessionRepository = activeSessionRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.gAuth = new GoogleAuthenticator();
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        // Admin Login
        if (adminEmail.equalsIgnoreCase(loginRequest.getEmail())) {
            if (passwordEncoder.matches(loginRequest.getPassword(), adminPasswordHash)) {
                // Check if MFA is set up for admin
                Optional<AdminConfig> configOpt = adminConfigRepository.findFirstByEmail(adminEmail);
                Map<String, Object> response = new HashMap<>();
                response.put("mfaRequired", true);
                
                String secret;
                if (configOpt.isEmpty() || configOpt.get().getMfaSecret() == null) {
                    // First time setup
                    GoogleAuthenticatorKey key = gAuth.createCredentials();
                    secret = key.getKey();
                    
                    AdminConfig config = configOpt.orElse(new AdminConfig());
                    config.setEmail(adminEmail);
                    config.setMfaSecret(secret);
                    adminConfigRepository.save(config);
                    
                    response.put("setupRequired", true);
                } else {
                    response.put("setupRequired", false);
                    secret = configOpt.get().getMfaSecret();
                }

                response.put("secret", secret);
                try {
                    String otpAuth = "otpauth://totp/EmpMonitor:" + adminEmail + "?secret=" + secret + "&issuer=EmpMonitor";
                    String encodedUrl = java.net.URLEncoder.encode(otpAuth, java.nio.charset.StandardCharsets.UTF_8.toString());
                    String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encodedUrl;
                    response.put("qrCode", qrUrl);
                } catch (Exception e) {
                    response.put("qrCode", "");
                }
                return ResponseEntity.ok(response);
            }
        }

        // Employee Login
        Optional<Employee> empOpt = employeeRepository.findFirstByEmail(loginRequest.getEmail());
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            if (passwordEncoder.matches(loginRequest.getPassword(), emp.getPassword())) {
                if (!loginRequest.isForce() && activeSessionRepository.findFirstByEmail(emp.getEmail()).isPresent()) {
                    Map<String, Object> errorResp = new HashMap<>();
                    errorResp.put("sessionActive", true);
                    errorResp.put("message", "Already one session is active. If you login here, the previous login will expire.");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResp);
                }

                String jwt = jwtTokenUtil.generateToken(emp.getEmail(), "EMPLOYEE", emp.getEmployeeCode());
                String refresh = jwtTokenUtil.generateRefreshToken(emp.getEmail());

                // Revoke old session and save new one
                activeSessionRepository.findFirstByEmail(emp.getEmail()).ifPresent(session -> {
                    activeSessionRepository.delete(session);
                });
                String userAgent = request.getHeader("User-Agent");
                ActiveSession session = new ActiveSession(emp.getEmail(), jwt, userAgent, LocalDateTime.now());
                activeSessionRepository.save(session);

                // Live dynamic notification for Employee Login
                try {
                    String clientIp = request.getHeader("X-Forwarded-For");
                    if (clientIp == null || clientIp.isBlank()) clientIp = request.getRemoteAddr();
                    if ("0:0:0:0:0:0:0:1".equals(clientIp)) clientIp = "127.0.0.1";
                    String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
                    notificationService.createNotification(
                        "Employee Login",
                        emp.getName() + " (" + emp.getEmployeeCode() + ") logged in at " + timeStr + (clientIp != null ? ". IP Address: " + clientIp : ""),
                        "Employee",
                        "login",
                        "/employees/view"
                    );
                } catch (Exception ignored) {}

                return ResponseEntity.ok(new LoginResponse(jwt, "EMPLOYEE", emp.getEmployeeCode(), emp.getName(), refresh));
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
    }

    @PostMapping("/reset-mfa")
    public ResponseEntity<?> resetMfa(@RequestBody LoginRequest loginRequest) {
        if (adminEmail.equalsIgnoreCase(loginRequest.getEmail())) {
            if (passwordEncoder.matches(loginRequest.getPassword(), adminPasswordHash)) {
                GoogleAuthenticatorKey key = gAuth.createCredentials();
                String secret = key.getKey();

                AdminConfig config = adminConfigRepository.findFirstByEmail(adminEmail).orElse(new AdminConfig());
                config.setEmail(adminEmail);
                config.setMfaSecret(secret);
                adminConfigRepository.save(config);

                Map<String, Object> resp = new HashMap<>();
                resp.put("secret", secret);
                try {
                    String otpAuth = "otpauth://totp/EmpMonitor:" + adminEmail + "?secret=" + secret + "&issuer=EmpMonitor";
                    String encodedUrl = java.net.URLEncoder.encode(otpAuth, java.nio.charset.StandardCharsets.UTF_8.toString());
                    String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encodedUrl;
                    resp.put("qrCode", qrUrl);
                } catch (Exception e) {
                    resp.put("qrCode", "");
                }
                return ResponseEntity.ok(resp);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest mfaRequest, HttpServletRequest request) {
        if (!adminEmail.equalsIgnoreCase(mfaRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email");
        }
        
        Optional<AdminConfig> configOpt = adminConfigRepository.findFirstByEmail(adminEmail);
        if (configOpt.isEmpty() || configOpt.get().getMfaSecret() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("MFA not set up");
        }
        
        try {
            int code = Integer.parseInt(mfaRequest.getCode());
            boolean isCodeValid = gAuth.authorize(configOpt.get().getMfaSecret(), code);
            
            if (isCodeValid) {
                if (!mfaRequest.isForce() && activeSessionRepository.findFirstByEmail(adminEmail).isPresent()) {
                    Map<String, Object> errorResp = new HashMap<>();
                    errorResp.put("sessionActive", true);
                    errorResp.put("message", "Already one session is active. If you login here, the previous login will expire.");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResp);
                }

                String jwt = jwtTokenUtil.generateToken(adminEmail, "ADMIN", null);
                String refresh = jwtTokenUtil.generateRefreshToken(adminEmail);

                // Revoke old session and save new one
                activeSessionRepository.findFirstByEmail(adminEmail).ifPresent(session -> {
                    activeSessionRepository.delete(session);
                });
                String userAgent = request.getHeader("User-Agent");
                ActiveSession session = new ActiveSession(adminEmail, jwt, userAgent, LocalDateTime.now());
                activeSessionRepository.save(session);

                // Live dynamic notification for Admin Login
                try {
                    String clientIp = request.getHeader("X-Forwarded-For");
                    if (clientIp == null || clientIp.isBlank()) clientIp = request.getRemoteAddr();
                    if ("0:0:0:0:0:0:0:1".equals(clientIp)) clientIp = "127.0.0.1";
                    String userAgentHeader = request.getHeader("User-Agent");
                    String deviceDesc = (userAgentHeader != null && userAgentHeader.contains("Windows")) ? "Windows PC" : "Web Browser";
                    notificationService.createNotification(
                        "New Login Detected",
                        "Admin logged in from " + deviceDesc + (clientIp != null ? " (" + clientIp + ")" : "") + ". If this wasn't you, please secure your account.",
                        "Security",
                        "login",
                        "/profile"
                    );
                } catch (Exception ignored) {}

                return ResponseEntity.ok(new LoginResponse(jwt, "ADMIN", null, "System Admin", refresh));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid verification code");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Code must be numeric");
        }
    }

    @PostMapping("/agent/login")
    public ResponseEntity<?> authenticateAgent(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        return authenticateUser(loginRequest, request);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody com.example.employee.dto.RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String refreshToken = request.getRefreshToken();
        
        if (jwtTokenUtil.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
        }

        String email = jwtTokenUtil.getUsernameFromToken(refreshToken);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        // Determine role and generate new token
        String jwt;
        if (adminEmail.equalsIgnoreCase(email)) {
            jwt = jwtTokenUtil.generateToken(email, "ADMIN", null);
        } else {
            Optional<Employee> empOpt = employeeRepository.findFirstByEmail(email);
            if (empOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }
            jwt = jwtTokenUtil.generateToken(email, "EMPLOYEE", empOpt.get().getEmployeeCode());
        }

        String newRefresh = jwtTokenUtil.generateRefreshToken(email);

        // Revoke old session and save new one
        activeSessionRepository.findFirstByEmail(email).ifPresent(session -> {
            activeSessionRepository.delete(session);
        });
        String userAgent = httpRequest.getHeader("User-Agent");
        ActiveSession session = new ActiveSession(email, jwt, userAgent, LocalDateTime.now());
        activeSessionRepository.save(session);

        return ResponseEntity.ok(new LoginResponse(jwt, adminEmail.equalsIgnoreCase(email) ? "ADMIN" : "EMPLOYEE", null, "Refreshed Token", newRefresh));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            activeSessionRepository.findByToken(jwt).ifPresent(session -> {
                // Live dynamic notification for Employee Offline
                try {
                    String email = session.getEmail();
                    if (email != null && !adminEmail.equalsIgnoreCase(email)) {
                        Optional<Employee> empOpt = employeeRepository.findFirstByEmail(email);
                        String name = empOpt.map(Employee::getName).orElse(email);
                        String code = empOpt.map(Employee::getEmployeeCode).orElse("");
                        String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
                        notificationService.createNotification(
                            "Employee Went Offline",
                            name + (code.isEmpty() ? "" : " (" + code + ")") + " went offline. Last active at " + timeStr + ".",
                            "Employee",
                            "offline",
                            "/employees/view"
                        );
                    }
                } catch (Exception ignored) {}
                activeSessionRepository.delete(session);
            });
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}
