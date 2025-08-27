// Service
package com.payment.service;


import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.payment.dto.CounterpartyRequest;
import com.payment.model.ExternalCounterparty;
import com.payment.repository.CounterpartyRepository;

import java.util.*;

@Service @RequiredArgsConstructor
public class CounterpartyService {
    private final CounterpartyRepository repo;

    public UUID add(String customerId, CounterpartyRequest req) {
    	
    	if (repo.existsByCustomerIdAndAccountNumber(customerId, req.getAccountNumber())) {
    		throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists ");
    		}
    	
        ExternalCounterparty cp = ExternalCounterparty.builder()
                .customerId(customerId)
                .nickname(req.getNickname())
                .holderName(req.getHolderName())
                .institutionNumber(req.getInstitutionNumber())
                .transitNumber(req.getTransitNumber())
                .accountNumber(req.getAccountNumber())
                .supportsAft(Boolean.TRUE.equals(req.getSupportsAft()==null? true: req.getSupportsAft()))
                .supportsRtr(Boolean.TRUE.equals(req.getSupportsRtr()== null ? true: req.getSupportsRtr()))
                .preferredRail(req.getPreferredRail() == null ? null :
                        ExternalCounterparty.PreferredRail.valueOf(req.getPreferredRail()))
                .status(ExternalCounterparty.Status.PENDING_VERIFICATION) // micro-deposits later
                .build();
        return repo.save(cp).getId();
    }

    public ExternalCounterparty get(UUID id) {
        return repo.findById(id).orElseThrow();
    }

    public List<ExternalCounterparty> list(String customerId) {
        return repo.findByCustomerId(customerId);
    }

    public void verify(UUID id) {
        ExternalCounterparty cp = get(id);
        cp.setStatus(ExternalCounterparty.Status.VERIFIED);
        repo.save(cp);
    }

    public void disable(UUID id) {
        ExternalCounterparty cp = get(id);
        cp.setStatus(ExternalCounterparty.Status.DISABLED);
        repo.save(cp);
    }
}
