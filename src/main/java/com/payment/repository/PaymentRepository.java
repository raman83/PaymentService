package com.payment.repository;

import com.payment.model.CanonicalPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<CanonicalPaymentEntity, Long> {
    List<CanonicalPaymentEntity> findTop20ByIncludedInAchFalseOrderByTimestampAsc();
}
