package com.common.iso;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  
public class CanonicalPayment {
	private UUID paymentId;
    private String debtorAccount;

    private String creditorName;
    private String creditorAccount;           // external account number (if AFT)
    private String creditorInstitutionNumber; // NEW
    private String creditorTransitNumber;     // NEW
    private String currency;
    private String purpose;

    private BigDecimal amount;
    private LocalDate requestedExecutionDate;
    private String channel; // ACH, RTR, SWIFT, etc.
    
    
 // --- RTR-specific (optional) ---
    private String proxyType;      // EMAIL, PHONE, BUSINESS_ID
    private String proxyValue;
    private String rtrStatus;      // PENDING, ACCEPTED, REJECTED
    private String rtrReasonCode;  // e.g. LIMIT_EXCEEDED, INVALID_PROXY
    
    
    
    // ✅ BILL-specific (optional)
    private String billerName;
    private String billReferenceNumber;
}
