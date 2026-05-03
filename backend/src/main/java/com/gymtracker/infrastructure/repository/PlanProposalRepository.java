package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.PlanProposal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanProposalRepository extends JpaRepository<PlanProposal, UUID> {

    List<PlanProposal> findByAttempt_IdOrderByVersionDesc(UUID attemptId);

    Optional<PlanProposal> findFirstByAttempt_IdOrderByVersionDesc(UUID attemptId);

    Optional<PlanProposal> findLatestByAttemptId(UUID attemptId);

    Optional<PlanProposal> findFirstByAttempt_IdOrderByVersionAsc(UUID attemptId);
}

