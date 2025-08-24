package com.payment.client.dto;


import lombok.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)

public class LedgerEntryResponse {
private UUID id;
private String side; // DEBIT/CREDIT
private BigDecimal amount;
private BigDecimal resultingLedgerBalance;
private LocalDateTime postedAt;
private String description;
private UUID externalTransactionId;
}