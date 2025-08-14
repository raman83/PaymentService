package com.payment.dto;

import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CounterpartyRequest {
    private String customerId;
    private String nickname;
    private String holderName;
    private String institutionNumber;
    private String transitNumber;
    private String accountNumber;
    private Boolean supportsAft;   // optional
    private Boolean supportsRtr;   // optional
    private String preferredRail;  // "AFT" or "RTR"
}