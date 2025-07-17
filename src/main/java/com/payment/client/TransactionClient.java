package com.payment.client;


import com.transaction.dto.TransactionRequest;
import com.transaction.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "transaction-service", url = "${transaction.service.url}") // adjust port for service discovery
public interface TransactionClient {

    @PostMapping("/api/v1/transactions")
    public ResponseEntity<TransactionResponse> create(@RequestBody TransactionRequest request);
}
