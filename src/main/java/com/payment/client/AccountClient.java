package com.payment.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import com.payment.client.dto.*;

import org.springframework.web.bind.annotation.*;



@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountClient {
	

	@GetMapping("/api/v1/accounts/{id}")
	ResponseEntity<AccountResponse> getAccount(@PathVariable("id") UUID id, @RequestHeader(value = "Authorization", required = false) String authorizationHeader);


	@PostMapping("/api/v1/accounts/{id}/debit")
	ResponseEntity<LedgerEntryResponse> debit(
	@PathVariable("id") UUID id,
	@RequestHeader(value = "Idempotency-Key", required = false) String idem,
	@RequestHeader(value = "If-Match", required = false) String ifMatch,
	@RequestBody PostingRequest request);


	@PostMapping("/api/v1/accounts/{id}/credit")
	ResponseEntity<LedgerEntryResponse> credit(
	@PathVariable("id") UUID id,
	@RequestHeader(value = "Idempotency-Key", required = false) String idem,
	@RequestHeader(value = "If-Match", required = false) String ifMatch,
	@RequestBody PostingRequest request, @RequestHeader(value = "Authorization", required = false) String authorizationHeader);


	@PostMapping("/api/v1/accounts/{id}/holds")
	ResponseEntity<HoldResponse> placeHold(
	@PathVariable("id") UUID id,
	@RequestHeader(value = "Idempotency-Key", required = false) String idem,
	@RequestBody CreateHoldRequest request);


	@PostMapping("/api/v1/accounts/{id}/holds/{holdId}/release")
	ResponseEntity<Void> releaseHold(
	@PathVariable("id") UUID id,
	@PathVariable("holdId") Long holdId,
	@RequestHeader(value = "If-Match", required = false) String ifMatch);


}
