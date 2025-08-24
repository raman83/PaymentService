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

    // Explicitly compute creditorAccount in the main mapping so it never ends up null for INTERNAL.
    @Mappings({
        @Mapping(target = "paymentId", ignore = true),
        @Mapping(target = "requestedExecutionDate",
                 expression = "java( request.getRequestedExecutionDate() != null ? request.getRequestedExecutionDate() : java.time.LocalDate.now() )"),
        @Mapping(target = "rtrStatus", ignore = true),
        @Mapping(target = "rtrReasonCode", ignore = true),
        // 👇 This is the important part
        @Mapping(target = "creditorAccount", expression = "java( resolveCreditorAccount(request) )"),
        // Optional: ensure channel string is set consistently
        @Mapping(target = "channel", expression = "java( request.getChannel() != null ? request.getChannel().name() : null )")
    })
    CanonicalPayment toCanonical(PaymentRequest request);

    CanonicalPaymentEntity toEntity(CanonicalPayment payment);

    CanonicalPayment toDto(CanonicalPaymentEntity entity);

    // ---------- Helpers ----------

    /**
     * For INTERNAL, use beneficiaryAccountId as the creditorAccount (UUID string).
     * For other channels, use whatever came in as creditorAccount (may be external account number).
     */
    default String resolveCreditorAccount(PaymentRequest req) {
        if (req.getChannel() == PaymentChannel.INTERNAL) {
            // PaymentRequest.beneficiaryAccountId is a String in your DTO; just return it.
            // (If you later change it to UUID, toString() will still be fine.)
            return req.getBeneficiaryAccountId();
        }
        return req.getCreditorAccount();
    }

    // If you still want the extra enrich logic (RTR status defaults, BILL fields),
    // keep AfterMapping BUT do not touch creditorAccount there again.
    @AfterMapping
    default void enrich(@MappingTarget CanonicalPayment p, PaymentRequest req) {
        // paymentId + requestedExecutionDate already handled in main mapping.

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
