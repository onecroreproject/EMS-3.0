package com.example.employee.controller.api.admin;

import com.example.employee.model.Notification;
import com.example.employee.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications() {
        List<Notification> list = notificationService.getAllNotifications();
        long unreadCount = notificationService.getUnreadCount();

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", list);
        response.put("unreadCount", unreadCount);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> toggleRead(@PathVariable String id) {
        Optional<Notification> updated = notificationService.toggleRead(id);
        return updated.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsRead();
        Map<String, String> res = new HashMap<>();
        res.put("message", "All notifications marked as read");
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable String id) {
        boolean deleted = notificationService.deleteNotification(id);
        if (deleted) {
            Map<String, String> res = new HashMap<>();
            res.put("message", "Notification deleted successfully");
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody Map<String, String> payload) {
        String title = payload.getOrDefault("title", "New Notification");
        String description = payload.getOrDefault("description", "");
        String category = payload.getOrDefault("category", "System");
        String iconType = payload.getOrDefault("iconType", "system");
        String link = payload.getOrDefault("link", "");

        Notification created = notificationService.createNotification(title, description, category, iconType, link);
        return ResponseEntity.ok(created);
    }
}
