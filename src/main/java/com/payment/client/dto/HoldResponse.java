package com.payment.client.dto;


import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)

public class HoldResponse {

	private Long id;
	private BigDecimal amount;
	private String currency;
	private String status;
	private String type;
	private String reason;
	private LocalDateTime releaseAt;
	private LocalDateTime createdAt;
}
