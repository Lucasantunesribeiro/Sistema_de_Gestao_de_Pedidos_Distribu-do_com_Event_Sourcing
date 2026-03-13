package com.ordersystem.unified.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentUserResponse {

    private String userId;
    private String username;
    private String email;
    private List<String> roles;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(String userId, String username, String email, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }
}
