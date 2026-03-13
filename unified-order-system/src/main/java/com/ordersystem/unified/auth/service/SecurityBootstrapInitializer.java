package com.ordersystem.unified.auth.service;

import com.ordersystem.common.security.SecurityProperties;
import com.ordersystem.unified.auth.model.ApplicationUser;
import com.ordersystem.unified.auth.repository.ApplicationUserRepository;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SecurityBootstrapInitializer implements ApplicationRunner {

    private final ApplicationUserRepository applicationUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public SecurityBootstrapInitializer(ApplicationUserRepository applicationUserRepository,
                                       PasswordEncoder passwordEncoder,
                                       SecurityProperties securityProperties) {
        this.applicationUserRepository = applicationUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (applicationUserRepository.count() > 0) {
            return;
        }

        SecurityProperties.BootstrapAdmin bootstrapAdmin = securityProperties.getBootstrapAdmin();
        if (!bootstrapAdmin.isEnabled()) {
            if (securityProperties.isEnforceAuthentication()) {
                throw new IllegalStateException(
                    "No application users found. Configure security.bootstrap-admin.* to create the first admin user."
                );
            }
            return;
        }

        if (!StringUtils.hasText(bootstrapAdmin.getUsername())
                || !StringUtils.hasText(bootstrapAdmin.getPassword())
                || !StringUtils.hasText(bootstrapAdmin.getEmail())) {
            throw new IllegalStateException(
                "security.bootstrap-admin.username, password and email are required when bootstrap admin is enabled"
            );
        }

        ApplicationUser admin = new ApplicationUser(
            UUID.randomUUID(),
            bootstrapAdmin.getUsername().trim(),
            passwordEncoder.encode(bootstrapAdmin.getPassword()),
            bootstrapAdmin.getEmail().trim(),
            String.join(",", bootstrapAdmin.getRoles()),
            true
        );

        applicationUserRepository.save(admin);
    }
}
