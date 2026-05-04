package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    java.util.Optional<User> findByUsername(String username);
}

