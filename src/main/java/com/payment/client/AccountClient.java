package com.payment.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountClient {
	@PostMapping("/api/v1/accounts/{id}/debit")
    public ResponseEntity<Void> debitAccount(
            @PathVariable("id") UUID id,
            @RequestParam("amount") BigDecimal amount);
	
	
	@PostMapping("/api/v1/accounts/{id}/credit")
	    void creditAccount(@PathVariable("id") UUID accountId, @RequestParam("amount") BigDecimal amount, @RequestHeader(value = "Authorization", required = false) String authorizationHeader);
}
