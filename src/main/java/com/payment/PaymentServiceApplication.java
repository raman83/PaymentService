package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

import com.commons.security.DefaultSecurityConfig;
import com.commons.security.FeignTokenRelayConfig;
@Import({DefaultSecurityConfig.class, FeignTokenRelayConfig.class})
@SpringBootApplication(scanBasePackages = {"com.commons", "com.payment"})
@EnableFeignClients(basePackages = "com.payment.client")
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
