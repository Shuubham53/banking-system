package com.banking.user_service.kafka;

import com.banking.user_service.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    private static final String TOPIC = "user-created";

    public void publishUserCreatedEvent(UserCreatedEvent event) {
        log.info("Publishing USER_CREATED event for userId: {}", event.getUserId());
        kafkaTemplate.send(TOPIC, event);
        log.info("USER_CREATED event published successfully");
    }
}
