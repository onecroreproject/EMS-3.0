package com.example.employee.controller;

import com.example.employee.dto.LoginRequest;
import com.example.employee.dto.LoginResponse;
import com.example.employee.dto.MfaVerifyRequest;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final AdminConfigRepository adminConfigRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthenticator gAuth;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password.hash}")
    private String adminPasswordHash;

    public AuthController(EmployeeRepository employeeRepository, AdminConfigRepository adminConfigRepository, JwtTokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.adminConfigRepository = adminConfigRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
        this.gAuth = new GoogleAuthenticator();
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Admin Login
        if (adminEmail.equalsIgnoreCase(loginRequest.getEmail())) {
            if (passwordEncoder.matches(loginRequest.getPassword(), adminPasswordHash)) {
                // Check if MFA is set up for admin
                Optional<AdminConfig> configOpt = adminConfigRepository.findByEmail(adminEmail);
                Map<String, Object> response = new HashMap<>();
                response.put("mfaRequired", true);
                
                if (configOpt.isEmpty() || configOpt.get().getMfaSecret() == null) {
                    // First time setup
                    GoogleAuthenticatorKey key = gAuth.createCredentials();
                    String secret = key.getKey();
                    
                    AdminConfig config = configOpt.orElse(new AdminConfig());
                    config.setEmail(adminEmail);
                    config.setMfaSecret(secret);
                    adminConfigRepository.save(config);
                    
                    response.put("setupRequired", true);
                    response.put("secret", secret);
                    
                    try {
                        String otpAuth = "otpauth://totp/EmpMonitor:" + adminEmail + "?secret=" + secret + "&issuer=EmpMonitor";
                        String encodedUrl = java.net.URLEncoder.encode(otpAuth, java.nio.charset.StandardCharsets.UTF_8.toString());
                        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encodedUrl;
                        response.put("qrCode", qrUrl);
                    } catch (Exception e) {
                        response.put("qrCode", "");
                    }
                } else {
                    response.put("setupRequired", false);
                }
                return ResponseEntity.ok(response);
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

    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest request) {
        if (!adminEmail.equalsIgnoreCase(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email");
        }
        
        Optional<AdminConfig> configOpt = adminConfigRepository.findByEmail(adminEmail);
        if (configOpt.isEmpty() || configOpt.get().getMfaSecret() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("MFA not set up");
        }
        
        try {
            int code = Integer.parseInt(request.getCode());
            boolean isCodeValid = gAuth.authorize(configOpt.get().getMfaSecret(), code);
            
            if (isCodeValid) {
                String jwt = jwtTokenUtil.generateToken(adminEmail, "ADMIN", null);
                String refresh = jwtTokenUtil.generateRefreshToken(adminEmail);
                return ResponseEntity.ok(new LoginResponse(jwt, "ADMIN", null, "System Admin", refresh));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid verification code");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Code must be numeric");
        }
    }

    @PostMapping("/agent/login")
    public ResponseEntity<?> authenticateAgent(@RequestBody LoginRequest loginRequest) {
        return authenticateUser(loginRequest);
    }
}
