package com.example.expensetracker.messaging;

import com.example.expensetracker.messaging.event.BudgetAlertEvent;
import com.example.expensetracker.notification.Notification;
import com.example.expensetracker.notification.NotificationRepository;
import com.example.expensetracker.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * STAGE 2 of the pipeline: turn an alert event into something the user actually sees.
 *
 * Two things worth pointing out:
 *  1. IDEMPOTENCY. Kafka is at-least-once. If this consumer saves the row and then dies before
 *     the offset is committed, the same event is redelivered on restart. The existsByEventId
 *     check (backed by a UNIQUE constraint) makes a duplicate notification impossible.
 *  2. It is a separate consumer GROUP from the budget evaluator, so both read the pipeline
 *     independently and can be scaled or restarted without affecting each other.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationConsumer(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.notification-events}",
            groupId = "notification-dispatcher",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onBudgetAlert(BudgetAlertEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate delivery for eventId={} - skipping", event.eventId());
            return;
        }

        Notification notification = new Notification();
        notification.setUser(userRepository.getReferenceById(event.userId()));
        notification.setEventId(event.eventId());
        notification.setType("BUDGET_" + event.severity());
        notification.setMessage(buildMessage(event));
        notification.setRead(false);
        notificationRepository.save(notification);

        // In production this is where an email/SMS/push provider would be called. Keeping it as a
        // log line means the project runs anywhere with no external account, and swapping in a real
        // provider later touches only this class.
        log.info("NOTIFICATION DISPATCHED -> to={} | {}", event.userEmail(), notification.getMessage());
    }

    private String buildMessage(BudgetAlertEvent event) {
        NumberFormat currency = NumberFormat.getNumberInstance(Locale.US);
        currency.setMinimumFractionDigits(2);

        String spend = currency.format(event.monthlySpend());
        String limit = currency.format(event.monthlyLimit());
        String category = event.category().name().charAt(0) + event.category().name().substring(1).toLowerCase();

        if ("EXCEEDED".equals(event.severity())) {
            return "You have exceeded your " + category + " budget for " + event.period()
                    + ". Spent " + spend + " of your " + limit + " limit (" + event.percentageUsed() + "%).";
        }
        return "Heads up: you have used " + event.percentageUsed() + "% of your " + category
                + " budget for " + event.period() + " (" + spend + " of " + limit + ").";
    }
}
