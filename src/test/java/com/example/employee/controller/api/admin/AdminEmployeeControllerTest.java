package com.example.employee.controller.api.admin;

import com.example.employee.model.Employee;
import com.example.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEmployeeController.class)
@Import({com.example.employee.config.SecurityConfig.class, com.example.employee.security.JwtRequestFilter.class, com.example.employee.security.JwtTokenUtil.class, com.example.employee.security.JwtUserDetailsService.class, com.example.employee.exception.GlobalExceptionHandler.class})
public class AdminEmployeeControllerTest {

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private com.example.employee.repository.EmployeeRepository employeeRepository;

    @MockBean
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Test
    @WithMockUser(username = "admin@company.com", roles = {"ADMIN"})
    public void testGetAllEmployees_Pagination() throws Exception {
        Employee mockEmployee = new Employee();
        mockEmployee.setId("1");
        mockEmployee.setName("Test Emp");
        mockEmployee.setEmail("test@test.com");

        java.util.List<Employee> employeeList = Collections.singletonList(mockEmployee);
        
        Mockito.when(mongoTemplate.count(Mockito.any(org.springframework.data.mongodb.core.query.Query.class), Mockito.eq(Employee.class))).thenReturn(1L);
        Mockito.when(mongoTemplate.find(Mockito.any(org.springframework.data.mongodb.core.query.Query.class), Mockito.eq(Employee.class))).thenReturn(employeeList);

        mockMvc.perform(get("/api/admin/employees?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Emp"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "admin@company.com", roles = {"ADMIN"})
    public void testCreateEmployee_ValidationFails() throws Exception {
        // Missing name and invalid email format
        String invalidEmployeeJson = "{\"email\":\"invalid-email\"}";

        mockMvc.perform(post("/api/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmployeeJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message.name").exists())
                .andExpect(jsonPath("$.message.email").exists());
    }
}
