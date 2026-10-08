package com.example.employee.security;

import com.example.employee.model.Employee;
import com.example.employee.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password.hash}")
    private String adminPasswordHash;

    public JwtUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check for Admin
        if (adminEmail.equalsIgnoreCase(username)) {
            return User.builder()
                    .username(adminEmail)
                    .password(adminPasswordHash)
                    .roles("ADMIN")
                    .build();
        }

        // Check for Employee
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(username);
        if (employeeOpt.isPresent()) {
            Employee emp = employeeOpt.get();
            return User.builder()
                    .username(emp.getEmail())
                    .password(emp.getPassword())
                    .roles("EMPLOYEE")
                    .build();
        }

        throw new UsernameNotFoundException("User not found with email: " + username);
    }
}
