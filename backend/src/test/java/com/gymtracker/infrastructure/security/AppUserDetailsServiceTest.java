package com.gymtracker.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gymtracker.domain.User;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService service;

    @Test
    void loadUserByUsername_existingUser_returnsCorrectUserDetails() {
        final UUID id = UUID.randomUUID();
        final User user = User.builder()
            .id(id)
            .username("admin")
            .password("admin")
            .preferredWeightUnit(WeightUnit.KG)
            .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        final UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("admin");
        assertThat(details.getAuthorities()).isNotEmpty();
    }

    @Test
    void loadUserByUsername_unknownUsername_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("unknown");
    }
}

