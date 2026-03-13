package com.ordersystem.unified.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Instant expiresAt;
    private CurrentUserResponse user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String tokenType, Instant expiresAt, CurrentUserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public CurrentUserResponse getUser() {
        return user;
    }
}
