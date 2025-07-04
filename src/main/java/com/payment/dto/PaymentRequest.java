package com.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
	 private String debtorName;
	    private String debtorAccount;
	    private String creditorName;
	    private String creditorAccount;
	    private String creditorBank;
	    private BigDecimal amount;
	    private String currency;
	    private String purpose;
	    private LocalDate requestedExecutionDate;
	    private PaymentChannel channel;

	    // RTR
	    private String proxyType;
	    private String proxyValue;

	    //  BILL
	    private String billerName;
	    private String billReferenceNumber;
}