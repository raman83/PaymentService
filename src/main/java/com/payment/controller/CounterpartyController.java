// Controller
package com.payment.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.commons.security.CurrentUser;
import com.payment.dto.CounterpartyRequest;
import com.payment.model.ExternalCounterparty;
import com.payment.service.CounterpartyService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

@RestController
@RequestMapping("/api/v1/counterparties")
@RequiredArgsConstructor
public class CounterpartyController {
    private final CounterpartyService service;
    private final CurrentUser currentUser;


    @PostMapping
    public ResponseEntity<Map<String, UUID>> add(@RequestBody CounterpartyRequest req,  HttpServletRequest http) {
        String customerId = currentUser.requireCustomerId(http);

    	UUID id = service.add(customerId,req);
        
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping
    public ResponseEntity<List<ExternalCounterparty>> list(HttpServletRequest http) {
        String customerId = currentUser.requireCustomerId(http);

        return ResponseEntity.ok(service.list(customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExternalCounterparty> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<Void> verify(@PathVariable("id") UUID id) {
        service.verify(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disable(@PathVariable UUID id) {
        service.disable(id);
        return ResponseEntity.ok().build();
    }
}
