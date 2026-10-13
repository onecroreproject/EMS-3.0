package com.example.employee.service;

import com.example.employee.model.Notification;
import com.example.employee.repository.NotificationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    public Notification createNotification(String title, String description, String category, String iconType, String link) {
        Notification notification = new Notification(title, description, category, iconType, link);
        return notificationRepository.save(notification);
    }

    public Optional<Notification> toggleRead(String id) {
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isPresent()) {
            Notification n = opt.get();
            n.setRead(!n.isRead());
            return Optional.of(notificationRepository.save(n));
        }
        return Optional.empty();
    }

    public Optional<Notification> markAsRead(String id) {
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isPresent()) {
            Notification n = opt.get();
            n.setRead(true);
            return Optional.of(notificationRepository.save(n));
        }
        return Optional.empty();
    }

    public void markAllAsRead() {
        List<Notification> list = notificationRepository.findAll();
        for (Notification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    public boolean deleteNotification(String id) {
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @PostConstruct
    public void seedInitialNotificationsIfEmpty() {
        if (notificationRepository.count() == 0) {
            long now = System.currentTimeMillis();

            Notification n1 = new Notification(
                    "New Login Detected",
                    "Admin logged in from a new device (Windows 11 - 192.168.1.25). If this wasn't you, please secure your account.",
                    "Security",
                    "login",
                    "/settings"
            );
            n1.setCreatedAt(new Date(now - (2 * 60 * 1000))); // 2 mins ago

            Notification n2 = new Notification(
                    "Employee Went Offline",
                    "John Doe (EMP001) went offline. Last active at 11:45 AM.",
                    "Employee",
                    "offline",
                    "/employees/view"
            );
            n2.setCreatedAt(new Date(now - (15 * 60 * 1000))); // 15 mins ago

            Notification n3 = new Notification(
                    "Task Assigned",
                    "A new task has been assigned to Smith (EMP002). Task: Update Project Documentation",
                    "Tasks",
                    "task",
                    "/tasks/track"
            );
            n3.setCreatedAt(new Date(now - (60 * 60 * 1000))); // 1 hour ago

            Notification n4 = new Notification(
                    "High Application Usage",
                    "Visual Studio Code usage is above 80% for Santhosh (EMP003). Usage Time: 5h 20m",
                    "System",
                    "usage",
                    "/time-tracking"
            );
            n4.setCreatedAt(new Date(now - (2 * 60 * 60 * 1000))); // 2 hours ago

            Notification n5 = new Notification(
                    "Idle Time Alert",
                    "Priya (EMP004) was idle for more than 30 minutes. Idle Duration: 35 minutes",
                    "Employee",
                    "idle",
                    "/tracking/attendance"
            );
            n5.setCreatedAt(new Date(now - (3 * 60 * 60 * 1000))); // 3 hours ago

            Notification n6 = new Notification(
                    "Employee Login",
                    "Santhosh (EMP003) logged in at 09:12 AM. IP Address: 192.168.1.10",
                    "Employee",
                    "login",
                    "/employees/view"
            );
            n6.setCreatedAt(new Date(now - (5 * 60 * 60 * 1000))); // 5 hours ago
            n6.setRead(true);

            Notification n7 = new Notification(
                    "New Employee Added",
                    "New employee Ravi Kumar (EMP005) has been added to the organization.",
                    "System",
                    "user",
                    "/employees/view"
            );
            n7.setCreatedAt(new Date(now - (24 * 60 * 60 * 1000))); // 1 day ago
            n7.setRead(true);

            notificationRepository.saveAll(List.of(n1, n2, n3, n4, n5, n6, n7));
        }
    }
}
