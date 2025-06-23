package com.payment.service;

import com.common.iso.CanonicalPayment;
import com.payment.client.AccountClient;
import com.payment.dto.PaymentRequest;
import com.payment.event.PaymentBatchReadyEvent;
import com.payment.model.CanonicalPaymentEntity;
import com.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AccountClient accountClient;

    private static final String ACH_TOPIC = "payment.batch.ready";
    private static final String RTR_TOPIC = "rtr.payment.requested";

    public void processPayment(PaymentRequest request) {
        // 1. Debit source account
        accountClient.debitAccount(request.getDebtorAccount(), request.getAmount());
        log.info("Debited {} from account {}", request.getAmount(), request.getDebtorAccount());

        // 2. Map to Canonical DTO
        CanonicalPayment paymentDto = mapToCanonicalPayment(request);

        // 3. Persist CanonicalPaymentEntity
        CanonicalPaymentEntity entity = mapToEntity(paymentDto);
        paymentRepository.save(entity);

        // 4. Handle based on payment channel
        switch (paymentDto.getChannel()) {
            case "RTR" -> handleRTR(paymentDto);
            case "ACH" -> handleACHBatching(entity);
            default -> log.warn("Unsupported payment channel: {}", paymentDto.getChannel());
        }
    }

    private CanonicalPayment mapToCanonicalPayment(PaymentRequest request) {
        return CanonicalPayment.builder()
                .paymentId(UUID.randomUUID().toString())
                .debtorName(request.getDebtorName())
                .debtorAccount(request.getDebtorAccount())
                .creditorName(request.getCreditorName())
                .creditorAccount(request.getCreditorAccount())
                .creditorBank(request.getCreditorBank())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .purpose(request.getPurpose())
                .requestedExecutionDate(LocalDate.now())
                .channel(request.getChannel()) // "ACH" or "RTR"
                .proxyType(request.getProxyType())       // null for ACH
                .proxyValue(request.getProxyValue())     // null for ACH
                .build();
    }

    private void handleRTR(CanonicalPayment payment) {
        kafkaTemplate.send(RTR_TOPIC, payment);
        log.info("Published RTR payment {} to RTR topic", payment.getPaymentId());
    }

    private void handleACHBatching(CanonicalPaymentEntity entity) {
        List<CanonicalPaymentEntity> pending = paymentRepository
                .findTop20ByIncludedInAchFalseOrderByTimestampAsc();

        if (pending.size() == 20) {
            pending.forEach(p -> p.setIncludedInAch(true));
            paymentRepository.saveAll(pending);

            List<CanonicalPayment> canonicalPayments = pending.stream()
                    .map(this::mapEntityToDto)
                    .toList();

            PaymentBatchReadyEvent event = new PaymentBatchReadyEvent(
                    UUID.randomUUID().toString(),
                    Instant.now().toString(),
                    canonicalPayments
            );

            kafkaTemplate.send(ACH_TOPIC, event);
            log.info("Published batch of 20 canonical payments to Kafka");
        }
    }

    private CanonicalPaymentEntity mapToEntity(CanonicalPayment dto) {
        CanonicalPaymentEntity entity = new CanonicalPaymentEntity();
        entity.setPaymentId(dto.getPaymentId());
        entity.setDebtorName(dto.getDebtorName());
        entity.setDebtorAccount(dto.getDebtorAccount());
        entity.setCreditorName(dto.getCreditorName());
        entity.setCreditorAccount(dto.getCreditorAccount());
        entity.setCreditorBank(dto.getCreditorBank());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency());
        entity.setPurpose(dto.getPurpose());
        entity.setRequestedExecutionDate(dto.getRequestedExecutionDate());
        entity.setChannel(dto.getChannel());
        entity.setTimestamp(Instant.now());
        entity.setIncludedInAch(false);
        return entity;
    }

    private CanonicalPayment mapEntityToDto(CanonicalPaymentEntity entity) {
        return CanonicalPayment.builder()
                .paymentId(entity.getPaymentId())
                .debtorName(entity.getDebtorName())
                .debtorAccount(entity.getDebtorAccount())
                .creditorName(entity.getCreditorName())
                .creditorAccount(entity.getCreditorAccount())
                .creditorBank(entity.getCreditorBank())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .purpose(entity.getPurpose())
                .requestedExecutionDate(entity.getRequestedExecutionDate())
                .channel(entity.getChannel())
                .build();
    }
}
