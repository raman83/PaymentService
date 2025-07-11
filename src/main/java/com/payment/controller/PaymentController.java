package com.payment.controller;

import com.payment.dto.PaymentRequest;
import com.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Void> makePayment(@RequestBody PaymentRequest request) {
        paymentService.processPayment(request);
        return ResponseEntity.ok().build();
    }
    
    
    
    @PutMapping("/payments/{paymentId}/status")
    public ResponseEntity<Void> updatePaymentStatus(
        @PathVariable UUID paymentId,
        @RequestParam String status,
        @RequestParam(required = false) String reason
    ) {
        paymentService.updateStatus(paymentId, status, reason);
        return ResponseEntity.ok().build();
    }
}
