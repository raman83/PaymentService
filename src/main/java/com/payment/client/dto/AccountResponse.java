package com.payment.client.dto;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)

public class AccountResponse {
private UUID id;
private String customerId;
private String accountType;
private String accountSubType;
private BigDecimal ledgerBalance;
private BigDecimal availableBalance;
private String currency;
private Integer version; // @Version surfaced by AccountService
}