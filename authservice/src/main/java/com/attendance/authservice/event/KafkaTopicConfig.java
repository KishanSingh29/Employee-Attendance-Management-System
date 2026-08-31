package com.attendance.authservice.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the {@code user-registered} topic so it is created automatically on
 * startup (single partition is enough for this project).
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userRegisteredTopic(
            @Value("${spring.kafka.topic.user-registered:user-registered}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
