package com.gymtracker.infrastructure.security;

import com.gymtracker.domain.User;
import com.gymtracker.infrastructure.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads user credentials from the {@code users} table for Spring Security.
 *
 * <p>Uses plain-text password comparison via {@code NoOpPasswordEncoder}
 * (MVP only).
 */
@Service
public final class AppUserDetailsService implements UserDetailsService {

    /** Repository used to look up users by username. */
    private final UserRepository userRepository;

    /**
     * Constructs the service.
     *
     * @param repo the user repository
     */
    public AppUserDetailsService(final UserRepository repo) {
        this.userRepository = repo;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UsernameNotFoundException if no user with the given username
     *     exists
     */
    @Override
    public UserDetails loadUserByUsername(
        final String username) throws UsernameNotFoundException {
        final User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
