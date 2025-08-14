package com.payment.dto;
import lombok.*;
import java.util.UUID;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
class CounterpartyResponse {
    private UUID id;
    private String customerId;
    private String nickname;
    private String holderName;
    private String institutionNumber;
    private String transitNumber;
    private String accountNumber;
    private boolean supportsAft;
    private boolean supportsRtr;
    private String preferredRail;
    private String status;
}