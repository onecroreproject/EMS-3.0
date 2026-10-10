package com.example.employee.controller;

import com.example.employee.model.Task;
import com.example.employee.repository.TaskRepository;
import com.example.employee.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskWebController {

    private final EmployeeService employeeService;
    private final TaskRepository taskRepository;

    public TaskWebController(EmployeeService employeeService, TaskRepository taskRepository) {
        this.employeeService = employeeService;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(taskRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        return taskRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            task.setAssignedBy(authentication.getName());
        }
        return ResponseEntity.ok(taskRepository.save(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable String id, @RequestBody Task task) {
        task.setId(id);
        Task existing = taskRepository.findById(id).orElse(null);
        if (existing != null) {
            if (task.getAssignedBy() == null || task.getAssignedBy().isEmpty()) {
                task.setAssignedBy(existing.getAssignedBy());
            }
        }
        return ResponseEntity.ok(taskRepository.save(task));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable String id) {
        taskRepository.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Task deleted successfully");
        return ResponseEntity.ok(response);
    }
}
