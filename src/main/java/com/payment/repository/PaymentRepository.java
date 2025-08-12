package com.payment.repository;

import com.payment.model.CanonicalPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<CanonicalPaymentEntity, UUID> {
    List<CanonicalPaymentEntity> findTop20ByIncludedInAchFalseOrderByTimestampAsc();
    List<CanonicalPaymentEntity> findTop20ByChannelAndIncludedInBillBatchFalseOrderByTimestampAsc(String channel);
    List<CanonicalPaymentEntity> findTop20ByChannelAndIncludedInAchFalseOrderByTimestampAsc(String channel);

    

    Optional<CanonicalPaymentEntity> findByPaymentId(UUID paymentId);

}
