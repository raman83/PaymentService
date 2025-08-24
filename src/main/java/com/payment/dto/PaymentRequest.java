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
	private String debtorAccount;      // hamara (from) account UUID string
	private String creditorAccount;                
    private String creditorBank;


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
    
    
    
 // Optional overrides
    private Boolean createHold; // force hold regardless of channel rules
    private Integer holdDays; // e.g., 7
    private String holdType; // CHEQUE, COMPLIANCE, etc.
    private String holdReason; 
    
    
}