package com.payment.service;

import org.springframework.stereotype.Service;

import com.payment.dto.BillerRequest;
import com.payment.model.Biller;
import com.payment.repository.BillerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillerService {

    private final BillerRepository billerRepository;

    public void addBiller(BillerRequest request) {
        Biller biller = Biller.builder()
                .name(request.getName())
                .referenceNumber(request.getReferenceNumber())
                .category(request.getCategory())
                .build();
        billerRepository.save(biller);
    }
}
