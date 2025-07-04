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
    default void enrich(@MappingTarget CanonicalPayment payment, PaymentRequest request) {
        payment.setPaymentId(UUID.randomUUID());


        if (payment.getRequestedExecutionDate() == null) {
            payment.setRequestedExecutionDate(LocalDate.now());
        }
     

        if (request.getChannel() == PaymentChannel.RTR) {
            payment.setRtrStatus("PENDING");
            payment.setRtrReasonCode(null);
        }

        if (request.getChannel() == PaymentChannel.BILL && request.getBillerName() != null) {
            payment.setCreditorName(request.getBillerName());
        }
        
        if (request.getChannel() == PaymentChannel.BILL) {
            // ✅ BILL enrichment
            if (request.getBillerName() != null) {
                payment.setBillerName(request.getBillerName());
                payment.setCreditorName(request.getBillerName()); // Overriding
            }
            if (request.getBillReferenceNumber() != null) {
                payment.setBillReferenceNumber(request.getBillReferenceNumber());
            }
        }
        
    }
}
