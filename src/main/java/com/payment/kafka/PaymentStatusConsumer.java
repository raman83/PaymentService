package com.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtr.dto.Pacs002Response;

import com.payment.dto.BillSettlementAckEvent;
import com.payment.dto.Camt002AckEvent;
import com.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "rtr.payment.status", groupId = "payment-dev", containerFactory = "kafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, Pacs002Response> record) {
        Pacs002Response payment = record.value();
        log.info("Received RTR payment: {}", payment);
        paymentService.updateStatus(UUID.fromString(payment.getOriginalPaymentId()),payment.getTransactionStatus(), payment.getReasonCode());
    }
    
    
    
    
    
    @KafkaListener(
            topics = "camt.002.ack",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "stringKafkaListenerContainerFactory"
        )
        public void handleCamt002Ack(ConsumerRecord<String, String> record) {
           
            
    	 String key = record.key();
         String payload = record.value();
         try {
        	 Camt002AckEvent ack =
                 objectMapper.readValue(payload, Camt002AckEvent.class);

             log.info("Received BILL status (key={} file={} count={})",
                     key, ack.getFileName(),
                     ack.getStatuses() != null ? ack.getStatuses().size() : 0);

             if (ack.getStatuses() == null) {
                 return;
             }

             for (Camt002AckEvent.PaymentAckStatus s : ack.getStatuses()) {
                 String st = s.getStatus() != null
                         ? s.getStatus().toUpperCase(Locale.ROOT)
                         : "REJECTED";

                 if ("ACCEPTED".equals(st) || "SETTLED".equals(st) || "SUCCESS".equals(st)) {
                     log.info("Marking payment {} as SETTLED", s.getPaymentId());
                     paymentService.updateStatus(s.getPaymentId(), "SETTLED", null);
                 } else {
                     log.info("Marking payment {} as FAILED (reason={})", s.getPaymentId(), s.getReason());
                     // Your PaymentService.updateStatus already does credit-back + reversal txn for FAILED
                     paymentService.updateStatus(s.getPaymentId(), "FAILED", s.getReason());
                 }
             }
         } catch (Exception e) {
             log.error("Failed to parse bill.payment.status payload: {}", payload, e);
         }
         
         
         
           

            
    
    }
    
    
    
    
    
    @KafkaListener(
            topics = "bill.payment.status",
            groupId = "payment-dev",
            containerFactory = "stringKafkaListenerContainerFactory"
        )
        public void onBillStatus(ConsumerRecord<String, String> record) {
            String key = record.key();
            String payload = record.value();
            try {
                BillSettlementAckEvent ack =
                    objectMapper.readValue(payload, BillSettlementAckEvent.class);

                log.info("Received BILL status (key={} file={} count={})",
                        key, ack.getFileName(),
                        ack.getStatuses() != null ? ack.getStatuses().size() : 0);

                if (ack.getStatuses() == null) {
                    return;
                }

                for (BillSettlementAckEvent.PaymentAckStatus s : ack.getStatuses()) {
                    String st = s.getStatus() != null
                            ? s.getStatus().toUpperCase(Locale.ROOT)
                            : "REJECTED";

                    if ("ACCEPTED".equals(st) || "SETTLED".equals(st) || "SUCCESS".equals(st)) {
                        log.info("Marking payment {} as SETTLED", s.getPaymentId());
                        paymentService.updateStatus(s.getPaymentId(), "SETTLED", null);
                    } else {
                        log.info("Marking payment {} as FAILED (reason={})", s.getPaymentId(), s.getReason());
                        // Your PaymentService.updateStatus already does credit-back + reversal txn for FAILED
                        paymentService.updateStatus(s.getPaymentId(), "FAILED", s.getReason());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse bill.payment.status payload: {}", payload, e);
            }
        }
    
    
    
    
    
    
}
