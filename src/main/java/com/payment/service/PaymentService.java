// PaymentService.java (updated)
package com.payment.service;

import com.common.iso.CanonicalPayment;
import com.payment.client.AccountClient;
import com.payment.client.TransactionClient;
import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentStatus;
import com.payment.event.BillPaymentBatchReadyEvent;
import com.payment.event.PaymentBatchReadyEvent;
import com.payment.mapper.PaymentMapper;
import com.payment.model.CanonicalPaymentEntity;
import com.payment.repository.PaymentRepository;
import com.transaction.dto.TransactionRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
    private final PaymentMapper mapper;

    private static final String ACH_TOPIC = "payment.batch.ready";
    private static final String RTR_TOPIC = "rtr.payment.requested";
    private static final String BILL_BATCH_READY_TOPIC = "bill.payment.batch.ready";

    public void processPayment(PaymentRequest request) {
        CanonicalPayment payment = mapper.toCanonical(request);

        // Debit source account
        accountClient.debitAccount(request.getDebtorAccount(), request.getAmount());

        // Log transaction
        postTransaction(payment);

        // Persist canonical record
        CanonicalPaymentEntity entity = mapper.toEntity(payment);
        entity.setTimestamp(Instant.now());
        entity.setStatus(PaymentStatus.PENDING);
        entity.setIncludedInAch(false);
        paymentRepository.save(entity);

        switch (request.getChannel()) {
            case AFT -> handleAftBatching();
            case RTR -> kafkaTemplate.send(RTR_TOPIC, payment);
            case BILL -> handleBillBatching();
            case INTERNAL -> handleInternalTransfer(payment);
        }
    }

    private void postTransaction(CanonicalPayment payment) {
        TransactionRequest txn = TransactionRequest.builder()
                .transactionId(UUID.randomUUID())
                .accountId(UUID.fromString(payment.getDebtorAccount()))
                .type("DEBIT")
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .postedDate(OffsetDateTime.now())
                .transactionDate(OffsetDateTime.now())
                .status("POSTED")
                .description("Payment to " + payment.getCreditorAccount())
                .build();
        transactionClient.create(txn);
    }

    private void postCreditTransaction(CanonicalPayment payment) {
        TransactionRequest txn = TransactionRequest.builder()
                .transactionId(UUID.randomUUID())
                .accountId(UUID.fromString(payment.getCreditorAccount()))
                .type("CREDIT")
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .postedDate(OffsetDateTime.now())
                .transactionDate(OffsetDateTime.now())
                .status("POSTED")
                .description("Received from " + payment.getDebtorAccount())
                .build();
        transactionClient.create(txn);
    }

    private void handleInternalTransfer(CanonicalPayment payment) {
        log.info("🔄 Performing internal transfer between {} and {}", payment.getDebtorAccount(), payment.getCreditorAccount());

        // Credit recipient account
        accountClient.creditAccount(payment.getCreditorAccount(), payment.getAmount());
        postCreditTransaction(payment);

        CanonicalPaymentEntity entity = mapper.toEntity(payment);
        entity.setTimestamp(Instant.now());
        entity.setStatus(PaymentStatus.PROCESSED);
        paymentRepository.save(entity);
    }

    private void handleAftBatching() {
        List<CanonicalPaymentEntity> pending = paymentRepository.findTop20ByIncludedInAchFalseOrderByTimestampAsc();

        if (pending.size() == 20) {
            pending.forEach(p -> p.setIncludedInAch(true));
            paymentRepository.saveAll(pending);

            List<CanonicalPayment> canonicalPayments = pending.stream()
                    .map(mapper::toDto)
                    .toList();

            PaymentBatchReadyEvent event = new PaymentBatchReadyEvent(
                    UUID.randomUUID(),
                    Instant.now().toString(),
                    canonicalPayments
            );

            kafkaTemplate.send(ACH_TOPIC, event);
        }
    }

    private void handleBillBatching() {
        List<CanonicalPaymentEntity> pending = paymentRepository
                .findTop20ByChannelAndIncludedInBillBatchFalseOrderByTimestampAsc("BILL");

        if (pending.size() == 20) {
            pending.forEach(p -> p.setIncludedInBillBatch(true));
            paymentRepository.saveAll(pending);

            List<CanonicalPayment> canonicalPayments = pending.stream()
                    .map(mapper::toDto)
                    .toList();

            BillPaymentBatchReadyEvent event = new BillPaymentBatchReadyEvent(
                    UUID.randomUUID(),
                    Instant.now().toString(),
                    canonicalPayments
            );

            kafkaTemplate.send(BILL_BATCH_READY_TOPIC, event);
        }
    }

    public void updateStatus(UUID paymentId, String status, String reason) {
        CanonicalPaymentEntity entity = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        entity.setStatus(PaymentStatus.valueOf(status));
        entity.setAckReceivedAt(Instant.now());
        paymentRepository.save(entity);
    }
}
