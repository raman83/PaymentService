package com.payment.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "external_counterparties",
       uniqueConstraints = @UniqueConstraint(name = "uq_owner_routing",
         columnNames = {"customerId", "institutionNumber", "transitNumber", "accountNumber"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalCounterparty {

    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String customerId;          // owner in our bank

    private String nickname;
    private String holderName;

    // Canadian EFT routing
    @Column(nullable = false, length = 3)
    private String institutionNumber;   // e.g. 004

    @Column(nullable = false, length = 5)
    private String transitNumber;       // e.g. 12345

    @Column(nullable = false)
    private String accountNumber;       // receiver acct at external bank

    private boolean supportsAft = true;
    private boolean supportsRtr = false;

    @Enumerated(EnumType.STRING)
    private PreferredRail preferredRail; // AFT or RTR (hint for UX)

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING_VERIFICATION, VERIFIED, DISABLED

    public enum PreferredRail { AFT, RTR }
    public enum Status { PENDING_VERIFICATION, VERIFIED, DISABLED }
}
