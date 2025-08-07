package com.payment.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.*;


@Entity
@Table(name = "billers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Biller {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String referenceNumber;
    private String category;
}
