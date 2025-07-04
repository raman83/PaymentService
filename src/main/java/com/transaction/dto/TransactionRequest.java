package com.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {
	
    private UUID transactionId;


    @NotNull
    private UUID accountId;

    @NotNull
    private String type;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String currency;

    @NotNull
    private OffsetDateTime postedDate;

    @NotNull
    private OffsetDateTime transactionDate;

    private String description;

    @NotNull
    private String status;

    private String category;

    private BigDecimal runningBalance;

    private String merchantName;

    private String referenceNumber;
}
