package com.payment.client;


import com.transaction.dto.TransactionRequest;
import com.transaction.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "transaction-service", url = "${transaction.service.url}") // adjust port for service discovery
public interface TransactionClient {

    @PostMapping("/api/v1/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request,@RequestHeader(value = "Authorization", required = false) String authorizationHeader);
}
