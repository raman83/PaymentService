package com.payment.controller;

import com.payment.dto.BillerRequest;
import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.service.BillerService;
import com.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BillerService billerService;


    @PostMapping
    public ResponseEntity<PaymentResponse> makePayment(@RequestBody PaymentRequest request) {
    	PaymentResponse res=   paymentService.processPayment(request);
  	  URI loc = URI.create("/api/v1/payments");

        return ResponseEntity.created(loc).body(res);
    }
    
    
    @PostMapping("/billers")
    public ResponseEntity<Void> addBiller(@RequestBody BillerRequest request) {
        billerService.addBiller(request);
        return ResponseEntity.ok().build();
    }
    
    
    
    @PutMapping("/{paymentId}/status")
    public ResponseEntity<Void> updatePaymentStatus(
        @PathVariable UUID paymentId,
        @RequestParam String status,
        @RequestParam(required = false) String reason
    ) {
        paymentService.updateStatus(paymentId, status, reason);
        return ResponseEntity.ok().build();
    }
}
