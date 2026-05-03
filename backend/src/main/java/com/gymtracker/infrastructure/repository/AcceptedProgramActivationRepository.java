package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.AcceptedProgramActivation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcceptedProgramActivationRepository extends JpaRepository<AcceptedProgramActivation, UUID> {

    Optional<AcceptedProgramActivation> findByProposalId(UUID proposalId);

    boolean existsByUserId(UUID userId);
}
