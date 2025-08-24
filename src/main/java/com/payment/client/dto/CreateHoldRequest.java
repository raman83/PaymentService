package com.payment.client.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class CreateHoldRequest {

	private BigDecimal amount;
	private String currency; // default from account if null
	private String type; // CARD, CHEQUE, COMPLIANCE, OTHER
	private String reason;
	/** Optional override in days; default 7 */
	private Integer releaseAfterDays;
	

}
