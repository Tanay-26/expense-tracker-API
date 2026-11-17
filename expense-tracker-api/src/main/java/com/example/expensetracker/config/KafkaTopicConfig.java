package com.example.expensetracker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares topics as code. Spring's KafkaAdmin creates any that are missing at startup, so a
 * teammate cloning the repo never has to run kafka-topics.sh by hand.
 *
 * 3 partitions = the unit of parallelism. One partition would cap us at one consumer thread
 * no matter how many instances we deploy. Replication factor 1 because this is a single-node
 * local broker; in production it would be 3.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic expenseEventsTopic(@Value("${app.kafka.topics.expense-events}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationEventsTopic(@Value("${app.kafka.topics.notification-events}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    // Dead-letter topics for messages that failed every retry.
    @Bean
    public NewTopic expenseEventsDlt(@Value("${app.kafka.topics.expense-events}") String name) {
        return TopicBuilder.name(name + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationEventsDlt(@Value("${app.kafka.topics.notification-events}") String name) {
        return TopicBuilder.name(name + ".DLT").partitions(3).replicas(1).build();
    }
}
