package com.common.iso;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  
public class CanonicalPayment {
    private String paymentId;
    private String debtorName;
    private String debtorAccount;
    private String creditorName;
    private String creditorAccount;
    private String creditorBank;
    private BigDecimal amount;
    private String currency;
    private String purpose;
    private LocalDate requestedExecutionDate;
    private String channel; // ACH, RTR, SWIFT, etc.
    
    
 // --- RTR-specific (optional) ---
    private String proxyType;      // EMAIL, PHONE, BUSINESS_ID
    private String proxyValue;
    private String rtrStatus;      // PENDING, ACCEPTED, REJECTED
    private String rtrReasonCode;  // e.g. LIMIT_EXCEEDED, INVALID_PROXY
}
