package com.rtr.dto;
import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pacs002Response {
    private String originalPaymentId;     // from pacs.008
    private String transactionStatus;     // ACCP or RJCT
    private String reasonCode;            // e.g., "NOFUNDS"
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private Instant settlementTimestamp;
}
