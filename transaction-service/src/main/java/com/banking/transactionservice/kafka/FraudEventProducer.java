package com.banking.transactionservice.kafka;

import com.banking.transactionservice.event.FraudAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventProducer {
    private final KafkaTemplate<String, FraudAlertEvent> kafkaTemplate;
    private static  final String TOPIC = "fraud-alert";
    public void publishFraudAlert(FraudAlertEvent event){
        log.warn("Publishing fraud alert event for account {}",event.getAccountNumber());
        kafkaTemplate.send(TOPIC,event);
    }
}
