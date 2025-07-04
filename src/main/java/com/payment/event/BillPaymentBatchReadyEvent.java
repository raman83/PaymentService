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
public class BillPaymentBatchReadyEvent {
    private UUID batchId;
    private String triggerTime;
    private List<CanonicalPayment> payments;
}
