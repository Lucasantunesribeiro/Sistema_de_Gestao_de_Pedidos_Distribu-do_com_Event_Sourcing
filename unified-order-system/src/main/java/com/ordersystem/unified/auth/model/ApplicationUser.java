package com.ordersystem.unified.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class ApplicationUser {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "roles", nullable = false)
    private String roles;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected ApplicationUser() {
    }

    public ApplicationUser(UUID id, String username, String passwordHash, String email, String roles, boolean enabled) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.roles = roles;
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getRoles() {
        return roles;
    }

    public List<String> getRoleList() {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
