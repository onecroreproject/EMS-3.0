package com.example.employee.controller;

import com.example.employee.model.Department;
import com.example.employee.model.Employee;
import com.example.employee.model.Team;
import com.example.employee.service.DepartmentService;
import com.example.employee.service.EmployeeService;
import com.example.employee.service.TeamService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Web (Thymeleaf) controller for Employee CRUD pages.
 * Replaces the static addViewController entries in WebMvcConfig so that
 * model attributes (${employee}, ${departments}, ${employees}) are properly
 * populated before the template is rendered.
 */
@Controller
@RequestMapping("/employees")
public class EmployeeWebController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final PasswordEncoder passwordEncoder;

    public EmployeeWebController(EmployeeService employeeService,
                                  DepartmentService departmentService,
                                  TeamService teamService,
                                  PasswordEncoder passwordEncoder) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.passwordEncoder = passwordEncoder;
    }

    // LIST
    /** GET /employees/list - show all employees */
    @GetMapping("/list")
    public String listEmployees(Model model) {
        List<Employee> employees = employeeService.findAll();
        employees.forEach(employeeService::enrichEmployeeWithDeptAndTeam);
        model.addAttribute("employees", employees);
        return "employee-list";
    }

    // ADD (new)
    /** GET /employees/new - show blank add-employee form */
    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("employee", new Employee());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("teams", teamService.getAll());
        return "employee-form";
    }

    // EDIT
    /** GET /employees/edit/{id} - show pre-filled edit form */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model) {
        Employee employee = employeeService.findById(id);
        if (employee == null) {
            return "redirect:/employees/list";
        }
        model.addAttribute("employee", employee);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("teams", teamService.getAll());
        return "employee-form";
    }

    // SAVE (create + update)
    /** POST /employees/save - persist a new or updated employee */
    @PostMapping("/save")
    public String saveEmployee(@ModelAttribute Employee employee,
                               RedirectAttributes redirectAttributes) {
        if (employee.getPassword() != null && !employee.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        } else if (employee.getId() != null) {
            Employee existing = employeeService.findById(employee.getId());
            if (existing != null) {
                employee.setPassword(existing.getPassword());
            }
        }
        employeeService.save(employee);
        redirectAttributes.addFlashAttribute("successMessage", "Employee saved successfully!");
        return "redirect:/employees/list";
    }

    // DELETE
    /** GET /employees/delete/{id} - delete an employee and redirect to list */
    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable String id,
                                 RedirectAttributes redirectAttributes) {
        employeeService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee deleted successfully!");
        return "redirect:/employees/list";
    }

    // TEAMS BY DEPARTMENT (used by JS in employee-form.html)
    /** GET /employees/teams/by-department/{deptId} - returns JSON list of teams */
    @GetMapping("/teams/by-department/{deptId}")
    @ResponseBody
    public List<Team> getTeamsByDepartment(@PathVariable String deptId) {
        return teamService.getTeamsByDepartment(deptId)
                .stream()
                .map(dto -> {
                    Team t = new Team();
                    t.setId(dto.getId());
                    t.setName(dto.getName());
                    return t;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}