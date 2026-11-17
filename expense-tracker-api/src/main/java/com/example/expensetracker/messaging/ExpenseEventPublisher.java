package com.example.expensetracker.messaging;

import com.example.expensetracker.messaging.event.BudgetAlertEvent;
import com.example.expensetracker.messaging.event.ExpenseCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * The producer side. KafkaTemplate.send() is asynchronous: it drops the record into an in-memory
 * batch and returns immediately, which is why publishing costs the API request roughly 1ms
 * instead of the 400ms a synchronous email call used to cost.
 *
 * The KEY is the userId. Kafka guarantees ordering within a partition, and the default partitioner
 * hashes the key - so all events for one user land on one partition and are processed in order.
 * Using a random key would let "expense created" and "expense deleted" be handled out of order.
 */
@Component
public class ExpenseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ExpenseEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String expenseTopic;
    private final String notificationTopic;

    public ExpenseEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                 @Value("${app.kafka.topics.expense-events}") String expenseTopic,
                                 @Value("${app.kafka.topics.notification-events}") String notificationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.expenseTopic = expenseTopic;
        this.notificationTopic = notificationTopic;
    }

    public void publishExpenseCreated(ExpenseCreatedEvent event) {
        send(expenseTopic, String.valueOf(event.userId()), event, event.eventId());
    }

    public void publishBudgetAlert(BudgetAlertEvent event) {
        send(notificationTopic, String.valueOf(event.userId()), event, event.eventId());
    }

    private void send(String topic, String key, Object payload, String eventId) {
        kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                // We log instead of throwing: a broker hiccup must never fail the user's HTTP request.
                log.error("Failed to publish eventId={} to topic={}", eventId, topic, ex);
            } else {
                log.info("Published eventId={} to {}-{} @offset {}", eventId, topic,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
