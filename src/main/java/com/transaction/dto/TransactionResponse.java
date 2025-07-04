package com.transaction.dto;


import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID transactionId;
    private UUID accountId;
    private String type;
    private AmountDTO amount;
    private OffsetDateTime postedDate;
    private OffsetDateTime transactionDate;
    private String description;
    private String status;
    private String category;
    private AmountDTO runningBalance;
    private String merchantName;
    private String referenceNumber;

}
