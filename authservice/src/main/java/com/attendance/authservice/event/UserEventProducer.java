package com.attendance.authservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes {@link UserRegisteredEvent}s to Kafka. Sending is asynchronous and a
 * broker failure is logged, never propagated, so a Kafka outage cannot fail user
 * registration.
 */
@Component
public class UserEventProducer {

    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String userRegisteredTopic;

    public UserEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                             @Value("${spring.kafka.topic.user-registered:user-registered}") String userRegisteredTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.userRegisteredTopic = userRegisteredTopic;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(userRegisteredTopic, event.userId(), event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish user-registered event for userId={}", event.userId(), ex);
                } else {
                    log.info("Published user-registered event for userId={} to topic={} partition={} offset={}",
                            event.userId(), userRegisteredTopic,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Unable to hand off user-registered event for userId={} to Kafka", event.userId(), ex);
        }
    }
}
