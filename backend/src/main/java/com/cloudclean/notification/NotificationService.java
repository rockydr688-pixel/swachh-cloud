package com.cloudclean.notification;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(String userId, String title, String message, String type, String actionUrl) {
        Notification notification = new Notification(userId, title, message, type, actionUrl);
        Notification saved = notificationRepository.save(notification);
        log.info("Emitted notification [{}] to user {}: {}", type, userId, title);
        return saved;
    }

    public Map<String, Object> getUserNotifications(String userId) {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("notifications", list);
        result.put("unreadCount", unreadCount);
        return result;
    }

    public Notification markAsRead(String userId, String id) {
        Notification n = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        n.setRead(true);
        return notificationRepository.save(n);
    }

    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
        log.info("Marked all notifications as read for user: {}", userId);
    }

    public void deleteNotification(String userId, String id) {
        Notification n = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        notificationRepository.delete(n);
    }
}
