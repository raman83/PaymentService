package com.payment.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillSettlementAckEvent {
    private String fileName;
    private Instant processedAt;
    private List<PaymentAckStatus> statuses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentAckStatus {
        private UUID paymentId;
        private String status; // "ACCEPTED" or "REJECTED"
        private String reason; // e.g., "AC01"
    }
}
