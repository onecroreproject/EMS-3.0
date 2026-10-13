package com.example.employee.repository;

import com.example.employee.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    long countByIsReadFalse();

    List<Notification> findByCategoryOrderByCreatedAtDesc(String category);
}
