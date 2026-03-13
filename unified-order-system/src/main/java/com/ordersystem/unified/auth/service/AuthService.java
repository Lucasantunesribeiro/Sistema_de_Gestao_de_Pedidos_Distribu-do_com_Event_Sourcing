package com.ordersystem.unified.auth.service;

import com.ordersystem.common.security.JwtTokenService;
import com.ordersystem.common.security.SecurityProperties;
import com.ordersystem.unified.auth.dto.CurrentUserResponse;
import com.ordersystem.unified.auth.dto.LoginRequest;
import com.ordersystem.unified.auth.dto.LoginResponse;
import com.ordersystem.unified.auth.model.ApplicationUser;
import com.ordersystem.unified.auth.repository.ApplicationUserRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final ApplicationUserRepository applicationUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SecurityProperties securityProperties;

    public AuthService(ApplicationUserRepository applicationUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       SecurityProperties securityProperties) {
        this.applicationUserRepository = applicationUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.securityProperties = securityProperties;
    }

    public LoginResponse authenticate(LoginRequest request) {
        ApplicationUser user = applicationUserRepository.findByUsernameIgnoreCase(request.getUsername())
            .or(() -> applicationUserRepository.findByEmailIgnoreCase(request.getUsername()))
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Instant expiresAt = Instant.now().plus(securityProperties.getTokenValidity());
        String token = jwtTokenService.createToken(user.getUsername(), Map.of(
            "roles", user.getRoleList(),
            "email", user.getEmail(),
            "userId", user.getId().toString()
        ));

        return new LoginResponse(
            token,
            "Bearer",
            expiresAt,
            new CurrentUserResponse(user.getId().toString(), user.getUsername(), user.getEmail(), user.getRoleList())
        );
    }

    public CurrentUserResponse getCurrentUser(String username) {
        ApplicationUser user = applicationUserRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        return new CurrentUserResponse(
            user.getId().toString(),
            user.getUsername(),
            user.getEmail(),
            user.getRoleList()
        );
    }
}
