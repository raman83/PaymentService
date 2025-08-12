package com.authuser.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenBankRequest {
    private String clientId;
    private String clientSecret;
    private String code;
    private String redirectUri;

    
   
}
