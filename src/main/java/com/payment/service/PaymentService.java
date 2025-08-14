// PaymentService.java (updated)
package com.payment.service;

import com.authuser.dto.LoginResponse;
import com.authuser.dto.TokenRequest;
import com.common.iso.CanonicalPayment;
import com.payment.client.AccountClient;
import com.payment.client.AuthClient;
import com.payment.client.TransactionClient;
import com.payment.dto.PaymentChannel;
import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentStatus;
import com.payment.event.BillPaymentBatchReadyEvent;
import com.payment.event.PaymentBatchReadyEvent;
import com.payment.mapper.PaymentMapper;
import com.payment.model.*;
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
    private final AuthClient authClient;
    private final TransactionClient transactionClient;
    private final PaymentMapper mapper;
    private final CounterpartyService counterpartyService;

    private static final String ACH_TOPIC = "payment.batch.ready";
    private static final String RTR_TOPIC = "rtr.payment.requested";
    private static final String BILL_BATCH_READY_TOPIC = "bill.payment.batch.ready";

    public void processPayment(PaymentRequest request) {
        CanonicalPayment payment = mapper.toCanonical(request);
        
        
        if (request.getCounterpartyId() != null) {
            var cp = counterpartyService.get(request.getCounterpartyId());
            if (cp.getStatus() != ExternalCounterparty.Status.VERIFIED)
                throw new IllegalStateException("Counterparty not verified");

            payment.setCreditorName(
                cp.getHolderName() != null ? cp.getHolderName() : cp.getNickname()
            );
            payment.setCreditorAccount(cp.getAccountNumber());
            payment.setCreditorInstitutionNumber(cp.getInstitutionNumber());
            payment.setCreditorTransitNumber(cp.getTransitNumber());

            // If user chose channel=null, use cp.preferredRail as default
            if (request.getChannel() == null && cp.getPreferredRail() != null) {
                request.setChannel(PaymentChannel.valueOf(cp.getPreferredRail().name()));
            }
        }


        // Debit source account
        accountClient.debitAccount(UUID.fromString(request.getDebtorAccount()), request.getAmount());

        // Log transaction
        postTransaction(payment);

        // Persist canonical record
        CanonicalPaymentEntity entity = mapper.toEntity(payment);
        entity.setTimestamp(Instant.now());
        entity.setStatus(PaymentStatus.PENDING);
        entity.setIncludedInAch(request.getChannel() == PaymentChannel.AFT ? false : null);
        entity.setIncludedInBillBatch(request.getChannel() == PaymentChannel.BILL ? false : null);
        
        paymentRepository.save(entity);
        
        payment.setPaymentId(entity.getPaymentId());

        switch (request.getChannel()) {
            case AFT -> handleAftBatching();
            case RTR -> {
            	 // guards
                if (request.getCounterpartyId() != null) {
                    var cp = counterpartyService.get(request.getCounterpartyId());
                    if (!cp.isSupportsRtr()) throw new IllegalStateException("Counterparty not RTR-enabled");
                } else if (payment.getProxyType() == null || payment.getProxyValue() == null) {
                    throw new IllegalArgumentException("RTR needs counterparty with RTR or proxyType+proxyValue");
                }
                
             // normalize PHONE/EMAIL proxy
                String normalized = com.payment.util.ProxyUtils.normalizeProxy(payment.getProxyType(), payment.getProxyValue());
                payment.setProxyValue(normalized);
                payment.setRtrStatus("PENDING");
                payment.setRtrReasonCode(null);

                kafkaTemplate.send("rtr.payment.requested", payment);
                
                }
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
        transactionClient.createTransaction(txn,null);
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
        transactionClient.createTransaction(txn,null);
    }

    private void handleInternalTransfer(CanonicalPayment payment) {
        log.info("🔄 Performing internal transfer between {} and {}", payment.getDebtorAccount(), payment.getCreditorAccount());

        // Credit recipient account
        accountClient.creditAccount(UUID.fromString(payment.getCreditorAccount()), payment.getAmount(),null);
        postCreditTransaction(payment);

     // 2) mark existing row PROCESSED/SETTLED (no duplicate insert)
        CanonicalPaymentEntity entity = paymentRepository.findByPaymentId(payment.getPaymentId())
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        entity.setStatus(PaymentStatus.SETTLED);       // or PROCESSED if you prefer
        entity.setAckReceivedAt(Instant.now());
        paymentRepository.save(entity);
    }

    private void handleAftBatching() {
        List<CanonicalPaymentEntity> pending =     paymentRepository.findTop20ByChannelAndIncludedInAchFalseOrderByTimestampAsc("AFT");


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
        List<CanonicalPaymentEntity> pending =     paymentRepository.findTop20ByChannelAndIncludedInBillBatchFalseOrderByTimestampAsc("BILL");


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
        
        TokenRequest request = new TokenRequest();
        request.setClientId("COElk9GHOmWS9L4MAQvscuxA49Cl4mfI");
        request.setClientSecret("zTgcKt-3ZgIik7q6SlVnv_abzfTzD91CBmtJq2jIFXDLBcRy4yYD-du3En2rJXWI");        
   
        
        String token = authClient.token(request).getBody().getAccessToken();        
        
        // Compensation on failure
        if (PaymentStatus.valueOf(status) == PaymentStatus.FAILED) {
            // Auto-credit back
            accountClient.creditAccount(UUID.fromString(entity.getDebtorAccount()), entity.getAmount(),"Bearer " + token);
            // Post reversal txn
            transactionClient.createTransaction(TransactionRequest.builder()
                .transactionId(UUID.randomUUID())
                .accountId(UUID.fromString(entity.getDebtorAccount()))
                .type("CREDIT")
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .postedDate(OffsetDateTime.now())
                .transactionDate(OffsetDateTime.now())
                .status("POSTED")
                .description("Reversal for failed payment " + paymentId)
                .build(), "Bearer " + token);
        }
        
        
        
        
    }
}
