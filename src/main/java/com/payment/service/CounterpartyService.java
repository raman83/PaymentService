// Service
package com.payment.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.payment.dto.CounterpartyRequest;
import com.payment.model.ExternalCounterparty;
import com.payment.repository.CounterpartyRepository;

import java.util.*;

@Service @RequiredArgsConstructor
public class CounterpartyService {
    private final CounterpartyRepository repo;

    public UUID add(CounterpartyRequest req) {
        ExternalCounterparty cp = ExternalCounterparty.builder()
                .customerId(req.getCustomerId())
                .nickname(req.getNickname())
                .holderName(req.getHolderName())
                .institutionNumber(req.getInstitutionNumber())
                .transitNumber(req.getTransitNumber())
                .accountNumber(req.getAccountNumber())
                .supportsAft(Boolean.TRUE.equals(req.getSupportsAft()))
                .supportsRtr(Boolean.TRUE.equals(req.getSupportsRtr()))
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
