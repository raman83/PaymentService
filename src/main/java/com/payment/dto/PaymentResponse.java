package com.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
private UUID paymentId;
private String status;
private UUID fromAccount;
private String toAccount;
private BigDecimal amount;
private String currency;
private String channel;
private String direction;
private Long holdId; // if created
private Instant createdAt;
}