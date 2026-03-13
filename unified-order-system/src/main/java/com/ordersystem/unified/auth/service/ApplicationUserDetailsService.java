package com.ordersystem.unified.auth.service;

import com.ordersystem.unified.auth.model.ApplicationUser;
import com.ordersystem.unified.auth.repository.ApplicationUserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ApplicationUserDetailsService implements UserDetailsService {

    private final ApplicationUserRepository applicationUserRepository;

    public ApplicationUserDetailsService(ApplicationUserRepository applicationUserRepository) {
        this.applicationUserRepository = applicationUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ApplicationUser user = applicationUserRepository.findByUsernameIgnoreCase(username)
            .or(() -> applicationUserRepository.findByEmailIgnoreCase(username))
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<SimpleGrantedAuthority> authorities = user.getRoleList().stream()
            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
            .map(SimpleGrantedAuthority::new)
            .toList();

        return User.withUsername(user.getUsername())
            .password(user.getPasswordHash())
            .authorities(authorities)
            .disabled(!user.isEnabled())
            .build();
    }
}
