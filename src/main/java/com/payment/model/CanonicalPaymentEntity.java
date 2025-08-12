package com.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.payment.dto.PaymentStatus;

@Entity
@Table(name = "canonical_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalPaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID paymentId;
    private String debtorName;
    private String debtorAccount;
    private String creditorName;
    private String creditorAccount;
    private String creditorBank;
    private BigDecimal amount;
    private String currency;
    private String purpose;
    private LocalDate requestedExecutionDate;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private Instant ackReceivedAt;

    private String channel;
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = true)

    private Boolean includedInAch;
    @Column(nullable = true)

    private Boolean includedInBillBatch;

}
