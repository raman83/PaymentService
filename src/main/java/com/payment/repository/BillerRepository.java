package com.payment.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.model.Biller;

public interface BillerRepository extends JpaRepository<Biller, UUID> {
}
