package com.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
	private String debtorAccount;                // hamara (from) account UUID string
    private BigDecimal amount;
    private String currency;
    private String purpose;
    private LocalDate requestedExecutionDate;
    private PaymentChannel channel;              // AFT, RTR, INTERNAL, BILL

    // --- INTERNAL only ---
    private String beneficiaryAccountId;           // <-- NEW: jisko credit karna hai

    // --- RTR optional ---
    private String proxyType;
    private String proxyValue;

    // --- BILL optional ---
    private String billerName;
    private String billReferenceNumber;

    // --- External ke liye (pehle bataya) ---
    private UUID counterpartyId;     
}