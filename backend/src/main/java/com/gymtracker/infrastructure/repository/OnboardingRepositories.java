package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.AcceptedProgramActivation;
import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import com.gymtracker.domain.ProposalFeedback;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProfileGoalOnboardingAttemptRepository extends JpaRepository<ProfileGoalOnboardingAttempt, UUID> {

    Optional<ProfileGoalOnboardingAttempt> findFirstByUserIdAndStatus(UUID userId, OnboardingAttemptStatus status);
}

interface PlanProposalRepository extends JpaRepository<PlanProposal, UUID> {

    List<PlanProposal> findByAttempt_IdOrderByVersionDesc(UUID attemptId);

    Optional<PlanProposal> findFirstByAttempt_IdOrderByVersionDesc(UUID attemptId);
}

interface ProposalFeedbackRepository extends JpaRepository<ProposalFeedback, UUID> {

    Optional<ProposalFeedback> findByProposalId(UUID proposalId);
}

interface AcceptedProgramActivationRepository extends JpaRepository<AcceptedProgramActivation, UUID> {

    Optional<AcceptedProgramActivation> findByProposalId(UUID proposalId);
}


