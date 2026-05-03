package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileGoalOnboardingAttemptRepository extends JpaRepository<ProfileGoalOnboardingAttempt, UUID> {

    Optional<ProfileGoalOnboardingAttempt> findFirstByUserIdAndStatus(UUID userId, OnboardingAttemptStatus status);

    Optional<ProfileGoalOnboardingAttempt> findLatestByUserIdAndStatus(UUID userId, OnboardingAttemptStatus status);

    Optional<ProfileGoalOnboardingAttempt> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, OnboardingAttemptStatus status);

    boolean existsByUserIdAndStatus(UUID userId, OnboardingAttemptStatus status);
}

