package com.payment.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountClient {
	@PostMapping("/api/v1/accounts/{id}/debit")
    public ResponseEntity<Void> debitAccount(
            @PathVariable("id") String id,
            @RequestParam("amount") BigDecimal amount);
}
