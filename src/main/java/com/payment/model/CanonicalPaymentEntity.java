package com.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "canonical_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalPaymentEntity {
    @Id
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
    private String channel;
    @Column(nullable = false)
    private Instant timestamp;
    
    private Boolean includedInAch;
}
