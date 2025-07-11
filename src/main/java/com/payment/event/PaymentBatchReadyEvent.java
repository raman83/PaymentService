// PaymentBatchReadyEvent.java
package com.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.common.iso.CanonicalPayment;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBatchReadyEvent {
    private UUID batchId;
    private String createdAt;
    private List<CanonicalPayment> payments;
}
