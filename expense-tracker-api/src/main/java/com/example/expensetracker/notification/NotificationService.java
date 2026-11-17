package com.example.expensetracker.notification;

import com.example.expensetracker.common.PageResponse;
import com.example.expensetracker.common.ResourceNotFoundException;
import com.example.expensetracker.notification.dto.NotificationResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        return PageResponse.from(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))),
                NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification " + notificationId + " not found"));
        n.setRead(true);
        return NotificationResponse.from(n);
    }
}
