package com.ordersystem.unified.auth.repository;

import com.ordersystem.unified.auth.model.ApplicationUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, UUID> {

    Optional<ApplicationUser> findByUsernameIgnoreCase(String username);

    Optional<ApplicationUser> findByEmailIgnoreCase(String email);
}
