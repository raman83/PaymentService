package com.payment.kafka;

import com.payment.event.PaymentBatchReadyEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PaymentBatchReadyEvent event) {
        kafkaTemplate.send("payment-batch-topic", event);
    }
}
