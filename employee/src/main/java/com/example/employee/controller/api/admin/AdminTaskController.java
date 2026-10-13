package com.example.employee.controller.api.admin;

import com.example.employee.model.Task;
import com.example.employee.repository.TaskRepository;
import com.example.employee.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public AdminTaskController(TaskRepository taskRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(taskRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        Optional<Task> opt = taskRepository.findById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Task>> getTasksByEmployee(@PathVariable String employeeId) {
        // Find tasks assigned to this employee code/id
        return ResponseEntity.ok(taskRepository.findByAssignedTo(employeeId));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        if (task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }
        Task saved = taskRepository.save(task);
        try {
            String assignee = (saved.getAssignedTo() != null && !saved.getAssignedTo().isEmpty()) 
                    ? saved.getAssignedTo() 
                    : "Employee";
            notificationService.createNotification(
                "Task Assigned",
                "A new task has been assigned to " + assignee + ". Task: " + saved.getTitle(),
                "Tasks",
                "task",
                "/tasks/track"
            );
        } catch (Exception ignored) {}
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable String id, @RequestBody Task taskDetails) {
        Optional<Task> opt = taskRepository.findById(id);
        if (opt.isPresent()) {
            Task task = opt.get();
            task.setTitle(taskDetails.getTitle());
            task.setDescription(taskDetails.getDescription());
            task.setStatus(taskDetails.getStatus());
            task.setPriority(taskDetails.getPriority());
            task.setEndDate(taskDetails.getEndDate());
            return ResponseEntity.ok(taskRepository.save(task));
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/{id}/assign")
    public ResponseEntity<Task> assignTask(@PathVariable String id, @RequestParam String employeeId) {
        Optional<Task> opt = taskRepository.findById(id);
        if (opt.isPresent()) {
            Task task = opt.get();
            task.setAssignedTo(employeeId);
            return ResponseEntity.ok(taskRepository.save(task));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        taskRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
