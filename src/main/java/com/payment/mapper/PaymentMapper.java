package com.payment.mapper;

import com.payment.dto.PaymentChannel;
import com.payment.dto.PaymentRequest;
import com.payment.model.CanonicalPaymentEntity;
import com.common.iso.CanonicalPayment;

import java.time.LocalDate;
import java.util.UUID;

import org.mapstruct.*;
@Mapper(componentModel = "spring")
public interface PaymentMapper {

	@Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "requestedExecutionDate", ignore = true)
    @Mapping(target = "rtrStatus", ignore = true)
    @Mapping(target = "rtrReasonCode", ignore = true)
    CanonicalPayment toCanonical(PaymentRequest request);

    CanonicalPaymentEntity toEntity(CanonicalPayment payment);

    CanonicalPayment toDto(CanonicalPaymentEntity entity);

    @AfterMapping
    default void enrich(@MappingTarget CanonicalPayment p, PaymentRequest req) {
    	 p.setPaymentId(UUID.randomUUID());
         if (p.getRequestedExecutionDate() == null) {
             p.setRequestedExecutionDate(LocalDate.now());
         }

         // INTERNAL ⇒ beneficiary ko creditorAccount me set kar do (UUID string)
         if (req.getChannel() == PaymentChannel.INTERNAL) {
             if (req.getBeneficiaryAccountId() == null) {
                 throw new IllegalArgumentException("beneficiaryAccountId is required for INTERNAL transfers");
             }
             p.setCreditorAccount(req.getBeneficiaryAccountId().toString());
         }

         if (req.getChannel() == PaymentChannel.RTR) {
             p.setRtrStatus("PENDING");
             p.setRtrReasonCode(null);
         }

         if (req.getChannel() == PaymentChannel.BILL) {
             if (req.getBillerName() != null) {
                 p.setBillerName(req.getBillerName());
                 p.setCreditorName(req.getBillerName());
             }
             if (req.getBillReferenceNumber() != null) {
                 p.setBillReferenceNumber(req.getBillReferenceNumber());
             }
         }
        
    }
}
