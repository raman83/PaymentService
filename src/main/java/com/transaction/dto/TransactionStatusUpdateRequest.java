package com.transaction.dto;

import lombok.Data;

@Data
public class TransactionStatusUpdateRequest {
    private String status; // SETTLED, FAILED
}
