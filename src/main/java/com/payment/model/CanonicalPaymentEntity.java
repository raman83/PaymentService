package com.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.payment.dto.PaymentStatus;

@Entity
@Table(
    name = "canonical_payments",
    indexes = {
        @Index(name = "idx_cp_channel_ach_ts", columnList = "channel,included_in_ach,timestamp"),
        @Index(name = "idx_cp_channel_bill_ts", columnList = "channel,included_in_bill_batch,timestamp")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    // --- Debtor ---
    @Column(name = "debtor_account", nullable = false)
    private String debtorAccount;

    // --- Creditor (resolved) ---
    @Column(name = "creditor_name")
    private String creditorName;

    @Column(name = "creditor_account")
    private String creditorAccount; // internal UUID (INTERNAL) ya external acct (AFT)

    @Column(name = "creditor_institution_number", length = 3)
    private String creditorInstitutionNumber;

    @Column(name = "creditor_transit_number", length = 5)
    private String creditorTransitNumber;

    // --- Payment details ---
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "purpose", length = 140)
    private String purpose;

    @Column(name = "requested_execution_date")
    private LocalDate requestedExecutionDate;

    // --- State ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status; // PENDING/PROCESSED/FAILED/SETTLED

    @Column(name = "ack_received_at")
    private Instant ackReceivedAt;

    // --- Meta ---
    @Column(name = "channel", nullable = false, length = 12) // AFT/RTR/INTERNAL/BILL
    private String channel;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    // --- Batching flags (nullable so non-applicable rails = NULL) ---
    @Column(name = "included_in_ach")
    private Boolean includedInAch;

    @Column(name = "included_in_bill_batch")
    private Boolean includedInBillBatch;

    @PrePersist
    void prePersist() {
        if (timestamp == null) timestamp = Instant.now();
    }
}
