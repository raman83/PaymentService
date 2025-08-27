package com.payment.controller;

import com.commons.security.CurrentUser;
import com.payment.dto.BillerRequest;
import com.payment.dto.BillerResponse;
import com.payment.dto.PageResponse;
import com.payment.dto.PaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.service.BillerService;
import com.payment.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BillerService billerService;
    private final CurrentUser currentUser;


    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> makePayment(@RequestBody PaymentRequest request) {
    	PaymentResponse res=   paymentService.processPayment(request);
  	  URI loc = URI.create("/api/v1/payments");

        return ResponseEntity.created(loc).body(res);
    }
    
    
    @PostMapping("/billers")
    public ResponseEntity<BillerResponse> create(
    @RequestBody BillerRequest request,
    HttpServletRequest http
    ) {
    String customerId = currentUser.requireCustomerId(http);
    return ResponseEntity.ok(billerService.create(customerId, request));
    }


    @GetMapping("/billers")
    public ResponseEntity<PageResponse<BillerResponse>> list(
    	    @RequestParam(name = "limit",  defaultValue = "10") int limit,
    	    @RequestParam(name = "offset", defaultValue = "0")  int offset,
    	    HttpServletRequest http
    	)  {
    String customerId = currentUser.requireCustomerId(http);
    return ResponseEntity.ok(billerService.list(customerId, Math.max(1, limit), Math.max(0, offset)));
    }


    @GetMapping("/billers/{id}")
    public ResponseEntity<BillerResponse> get(
    @PathVariable UUID id,
    HttpServletRequest http
    ) {
    String customerId = currentUser.requireCustomerId(http);
    return ResponseEntity.ok(billerService.get(customerId, id));
    }


    @DeleteMapping("/billers/{id}")
    public ResponseEntity<Void> delete(
    @PathVariable UUID id,
    HttpServletRequest http
    ) {
    String customerId = currentUser.requireCustomerId(http);
    billerService.delete(customerId, id);
    return ResponseEntity.noContent().build();
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
