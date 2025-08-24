package com.payment.account;

import com.payment.client.AccountClient;
import com.payment.client.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountPostingAdapter {

    private final AccountClient accountClient;

    /** Fetch latest ETag for If-Match */
    private String etag(UUID accountId) {
        ResponseEntity<AccountResponse> resp = accountClient.getAccount(accountId,null);
        String etag = resp.getHeaders() != null ? resp.getHeaders().getETag() : null;
        if (etag == null || etag.isBlank()) {
            throw new IllegalStateException("Missing ETag for account " + accountId);
        }
        return etag;
    }

    public void debitInternal(UUID accountId, BigDecimal amount, String currency,
                              String description, String idempotencyKeySuffix) {
        String e = etag(accountId);
        PostingRequest dr = PostingRequest.builder()
                .amount(amount)
                .currency(currency)
                .description(description)
                .source("INTERNAL")
                .createHold(false)
                .build();
        accountClient.debit(accountId, idempotencyKeySuffix, e, dr);
        log.info("[PAYMENT] debit INTERNAL ok acc={} amt={} {}", accountId, amount, currency);
    }

    public void creditInternal(UUID accountId, BigDecimal amount, String currency,
                               String description, String idempotencyKeySuffix) {
        String e = etag(accountId);
        PostingRequest cr = PostingRequest.builder()
                .amount(amount)
                .currency(currency)
                .description(description)
                .source("INTERNAL")
                .createHold(false)
                .build();
        accountClient.credit(accountId, idempotencyKeySuffix, e, cr,null);
        log.info("[PAYMENT] credit INTERNAL ok acc={} amt={} {}", accountId, amount, currency);
    }

    /**
     * External inbound credit pattern:
     * 1) Place an explicit hold (so we know holdId)
     * 2) Credit WITHOUT auto-hold
     * Settlement will release hold later.
     */
    public Long creditExternalWithHold(UUID accountId, BigDecimal amount, String currency,
                                       String description, String idempotencyKeyBase,
                                       Integer holdDays, String holdType, String holdReason) {
        // 1) Hold
        CreateHoldRequest hc = CreateHoldRequest.builder()
                .amount(amount)
                .currency(currency)
                .type(holdType != null ? holdType : "CHEQUE")
                .reason(holdReason != null ? holdReason : "External inbound hold")
                .releaseAfterDays(holdDays != null && holdDays > 0 ? holdDays : 7)
                .build();
        ResponseEntity<HoldResponse> hold = accountClient.placeHold(accountId, idempotencyKeyBase + "|HOLD", hc);
        Long holdId = hold.getBody() != null ? hold.getBody().getId() : null;
        log.info("[PAYMENT] placed HOLD acc={} holdId={} amt={} {}", accountId, holdId, amount, currency);

        // 2) Credit w/o auto-hold
        String e = etag(accountId);
        PostingRequest cr = PostingRequest.builder()
                .amount(amount)
                .currency(currency)
                .description(description)
                .source("EXTERNAL")
                .createHold(false) // critical: we already created hold explicitly
                .build();
        accountClient.credit(accountId, idempotencyKeyBase + "|CREDIT", e, cr,null);
        log.info("[PAYMENT] credit EXTERNAL (no auto-hold) acc={} amt={} {}", accountId, amount, currency);
        return holdId;
    }

    /** External outbound debit (no hold) */
    public void debitExternalOutbound(UUID accountId, BigDecimal amount, String currency,
                                      String description, String idempotencyKeySuffix) {
        String e = etag(accountId);
        PostingRequest dr = PostingRequest.builder()
                .amount(amount)
                .currency(currency)
                .description(description)
                .source("INTERNAL")
                .createHold(false)
                .build();
        accountClient.debit(accountId, idempotencyKeySuffix, e, dr);
        log.info("[PAYMENT] debit OUTBOUND ok acc={} amt={} {}", accountId, amount, currency);
    }
}
