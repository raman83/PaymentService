package com.payment.kafka;

import com.common.iso.CanonicalPayment;
import com.rtr.dto.Pacs002Response;
import com.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusConsumer {

    private final PaymentService paymentService;


    @KafkaListener(topics = "rtr.payment.status", groupId = "payment-dev", containerFactory = "kafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, Pacs002Response> record) {
        Pacs002Response payment = record.value();
        log.info("Received RTR payment: {}", payment);
        paymentService.updateStatus(UUID.fromString(payment.getOriginalPaymentId()),payment.getTransactionStatus(), payment.getReasonCode());
    }
}
