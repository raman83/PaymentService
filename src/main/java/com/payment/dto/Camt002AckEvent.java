package com.payment.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Camt002AckEvent {
    private String fileName;
    private String status; // ACCEPTED, REJECTED, PARTIALLY_ACCEPTED
    private Instant ackReceivedAt;
    private List<PaymentAckStatus> statuses;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentAckStatus {
        private UUID paymentId;
        private String status; // ACCEPTED, REJECTED
        private String reason; // Optional
    }
    
}