package com.example.employee.controller;

import com.example.employee.model.Task;
import com.example.employee.repository.TaskRepository;
import com.example.employee.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequestMapping("/tasks")
public class TaskWebController {

    private final EmployeeService employeeService;
    private final TaskRepository taskRepository;

    public TaskWebController(EmployeeService employeeService, TaskRepository taskRepository) {
        this.employeeService = employeeService;
        this.taskRepository = taskRepository;
    }

    @GetMapping("/new")
    public String showTaskForm(Model model) {
        model.addAttribute("task", new Task());
        model.addAttribute("employees", employeeService.findAll());
        return "task-form";
    }
    
    @GetMapping("/edit/{id}")
    public String editTaskForm(@PathVariable String id, Model model) {
        Task task = taskRepository.findById(id).orElse(new Task());
        model.addAttribute("task", task);
        model.addAttribute("employees", employeeService.findAll());
        return "task-form";
    }

    @GetMapping({"", "/", "/list"})
    public String showTaskList() {
        return "task-list";
    }

    @PostMapping
    public String saveTask(@ModelAttribute Task task) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            task.setAssignedBy(authentication.getName());
        }
        
        taskRepository.save(task);
        return "redirect:/tasks/list";
    }

    @PostMapping("/{id}")
    public String updateTask(@PathVariable String id, @ModelAttribute Task task) {
        task.setId(id);
        
        Task existing = taskRepository.findById(id).orElse(null);
        if (existing != null) {
            if (task.getAssignedBy() == null || task.getAssignedBy().isEmpty()) {
                task.setAssignedBy(existing.getAssignedBy());
            }
        }
        
        taskRepository.save(task);
        return "redirect:/tasks/list";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteTask(@PathVariable String id) {
        taskRepository.deleteById(id);
        return "redirect:/tasks/list";
    }
}
