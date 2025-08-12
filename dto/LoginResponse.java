package com.authuser.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String idToken;
    private String tokenType;
    private Long expiresIn;
}
