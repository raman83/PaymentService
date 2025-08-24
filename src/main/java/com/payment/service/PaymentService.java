package com.payment.service;

import com.authuser.dto.TokenRequest;
import com.common.iso.CanonicalPayment;
import com.payment.client.AccountClient;
import com.payment.client.AuthClient;
import com.payment.client.TransactionClient;
import com.payment.client.dto.AccountResponse;           // for ETag access
import com.payment.client.dto.PostingRequest;           // skinny PostingRequest
import com.payment.dto.PaymentChannel;
import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentResponse;                  // <-- NEW
import com.payment.dto.PaymentStatus;
import com.payment.event.BillPaymentBatchReadyEvent;
import com.payment.event.PaymentBatchReadyEvent;
import com.payment.mapper.PaymentMapper;
import com.payment.model.*;
import com.payment.repository.PaymentRepository;
import com.transaction.dto.TransactionRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    /** Now returns PaymentResponse (everything else unchanged) */
    public PaymentResponse processPayment(PaymentRequest request) {
        CanonicalPayment payment = mapper.toCanonical(request);

        // ---- Counterparty enrichment (unchanged) ----
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

        // ---- DEBIT source account (ETag + PostingRequest) ----
        UUID debtorAccountId = UUID.fromString(request.getDebtorAccount());
        String eTagDebtor = fetchEtag(debtorAccountId, null);
        PostingRequest debitReq = PostingRequest.builder()
                .amount(request.getAmount())
                .currency(nz(request.getCurrency(), "CAD"))
                .description("Payment to " + nz(payment.getCreditorAccount(), "external"))
                .source("INTERNAL")
                .createHold(false)
                .build();
        accountClient.debit(debtorAccountId, null, eTagDebtor, debitReq);

        // ---- Log DEBIT transaction (unchanged) ----
        postTransaction(payment);

        // ---- Persist canonical record (unchanged) ----
        CanonicalPaymentEntity entity = mapper.toEntity(payment);
        entity.setTimestamp(Instant.now());
        entity.setStatus(PaymentStatus.PENDING);
        entity.setIncludedInAch(request.getChannel() == PaymentChannel.AFT ? false : null);
        entity.setIncludedInBillBatch(request.getChannel() == PaymentChannel.BILL ? false : null);
        paymentRepository.save(entity);

        payment.setPaymentId(entity.getPaymentId());

        // ---- Channel orchestration (unchanged logic) ----
        if (request.getChannel() != null) {
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

                    kafkaTemplate.send(RTR_TOPIC, payment);
                }
                case BILL -> handleBillBatching();
                case INTERNAL -> handleInternalTransfer(payment);
            }
        }

        // Re-read status if INTERNAL may have settled inside handleInternalTransfer
        CanonicalPaymentEntity latest = paymentRepository.findByPaymentId(payment.getPaymentId())
                .orElse(entity);

        // Build PaymentResponse (no holds created here; holdId remains null)
        String toAccount = null;
        if (request.getChannel() == PaymentChannel.INTERNAL && payment.getCreditorAccount() != null) {
            toAccount = payment.getCreditorAccount();
        }
        if (request.getChannel() == PaymentChannel.BILL && payment.getCreditorAccount() != null) {
            toAccount = payment.getCreditorAccount();
        }

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(latest.getPaymentId())
                .status(latest.getStatus() != null ? latest.getStatus().name() : null)
                .fromAccount(debtorAccountId)
                .toAccount(toAccount)
                .amount(latest.getAmount())
                .currency(latest.getCurrency())
                .channel(request.getChannel() != null ? request.getChannel().name() : null)
                .direction("OUTBOUND")
                .holdId(null)
                .createdAt(latest.getTimestamp())
                .build();

        return response;
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
        transactionClient.createTransaction(txn, null);
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
        transactionClient.createTransaction(txn, null);
    }

    private void handleInternalTransfer(CanonicalPayment payment) {
        log.info("🔄 Performing internal transfer between {} and {}", payment.getDebtorAccount(), payment.getCreditorAccount());

        // CREDIT recipient account (ETag + PostingRequest, INTERNAL no hold)
        UUID creditorAccountId = UUID.fromString(payment.getCreditorAccount());
        String eTagCreditor = fetchEtag(credorOrThrow(creditorAccountId), null);
        PostingRequest creditReq = PostingRequest.builder()
                .amount(payment.getAmount())
                .currency(nz(payment.getCurrency(), "CAD"))
                .description("Internal transfer credit from " + payment.getDebtorAccount())
                .source("INTERNAL")
                .createHold(false)
                .build();
        accountClient.credit(creditorAccountId, null, eTagCreditor, creditReq, null);
        // Log CREDIT transaction
        postCreditTransaction(payment);

        // Mark settled
        CanonicalPaymentEntity entity = paymentRepository.findByPaymentId(payment.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        entity.setStatus(PaymentStatus.SETTLED);
        entity.setAckReceivedAt(Instant.now());
        paymentRepository.save(entity);
    }

    private void handleAftBatching() {
        List<CanonicalPaymentEntity> pending =
                paymentRepository.findTop20ByChannelAndIncludedInAchFalseOrderByTimestampAsc("AFT");

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
        List<CanonicalPaymentEntity> pending =
                paymentRepository.findTop20ByChannelAndIncludedInBillBatchFalseOrderByTimestampAsc("BILL");

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

        // Compensation on failure (preserved) — but use new credit API with If-Match
        if (PaymentStatus.valueOf(status) == PaymentStatus.FAILED) {
            UUID debtor = UUID.fromString(entity.getDebtorAccount());
            String eTag = fetchEtag(debtor,"Bearer " + token );
            PostingRequest creditBack = PostingRequest.builder()
                    .amount(entity.getAmount())
                    .currency(nz(entity.getCurrency(), "CAD"))
                    .description("Reversal for failed payment " + paymentId)
                    .source("INTERNAL")
                    .createHold(false)
                    .build();
            accountClient.credit(debtor, null, eTag, creditBack, "Bearer " + token);

            // Post reversal txn
            transactionClient.createTransaction(TransactionRequest.builder()
                    .transactionId(UUID.randomUUID())
                    .accountId(debtor)
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

    // ---------------- helpers ----------------

    /** Fetch ETag for an account using GET /accounts/{id} */
    private String fetchEtag(UUID accountId, String token) {
        ResponseEntity<AccountResponse> resp = accountClient.getAccount(accountId,token);
        String etag = resp.getHeaders() != null ? resp.getHeaders().getETag() : null;
        if (etag == null || etag.isBlank()) {
            throw new IllegalStateException("Missing ETag for account " + accountId);
        }
        return etag;
    }

    private static String nz(String val, String def) {
        return (val == null || val.isBlank()) ? def : val;
    }

    private static UUID credorOrThrow(UUID id) {
        if (id == null) throw new IllegalArgumentException("Creditor account required for INTERNAL");
        return id;
    }
}
