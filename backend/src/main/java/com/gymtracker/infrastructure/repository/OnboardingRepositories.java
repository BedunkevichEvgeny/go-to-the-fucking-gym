package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.ProposalFeedback;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProposalFeedbackRepository extends JpaRepository<ProposalFeedback, UUID> {

    Optional<ProposalFeedback> findByProposalId(UUID proposalId);
}

