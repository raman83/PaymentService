package com.payment.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.authuser.dto.LoginResponse;
import com.authuser.dto.TokenRequest;

@FeignClient(name = "auth-user", url = "${authuser.service.url}")
public interface AuthClient {
    
    @PostMapping("/api/v1/auth/m2mToken")
    public ResponseEntity<LoginResponse> token(@RequestBody TokenRequest request);
    
}
