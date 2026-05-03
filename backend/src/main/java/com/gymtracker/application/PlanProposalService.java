package com.gymtracker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.GeneratedBy;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingAttemptResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.TrackingAccessGateResponse;
import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import com.gymtracker.infrastructure.ai.OnboardingPlanGenerator;
import com.gymtracker.infrastructure.mapper.OnboardingProposalMapper;
import com.gymtracker.infrastructure.repository.AcceptedProgramActivationRepository;
import com.gymtracker.infrastructure.repository.PlanProposalRepository;
import com.gymtracker.infrastructure.repository.ProfileGoalOnboardingAttemptRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanProposalService {

    private static final Logger logger = LoggerFactory.getLogger(PlanProposalService.class);

    private final OnboardingValidationService validationService;
    private final OnboardingPlanGenerator onboardingPlanGenerator;
    private final ProfileGoalOnboardingAttemptRepository attemptRepository;
    private final PlanProposalRepository proposalRepository;
    private final AcceptedProgramActivationRepository activationRepository;
    private final OnboardingProposalMapper proposalMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlanProposalService(
            OnboardingValidationService validationService,
            OnboardingPlanGenerator onboardingPlanGenerator,
            ProfileGoalOnboardingAttemptRepository attemptRepository,
            PlanProposalRepository proposalRepository,
            AcceptedProgramActivationRepository activationRepository,
            OnboardingProposalMapper proposalMapper) {
        this.validationService = validationService;
        this.onboardingPlanGenerator = onboardingPlanGenerator;
        this.attemptRepository = attemptRepository;
        this.proposalRepository = proposalRepository;
        this.activationRepository = activationRepository;
        this.proposalMapper = proposalMapper;
    }

    @Transactional
    public PlanProposalResponse createInitialProposal(UUID userId, OnboardingSubmissionRequest request) {
        validationService.validate(request);

        PlanProposalResponse proposal = onboardingPlanGenerator.generateInitialProposal(userId, request);

        ProfileGoalOnboardingAttempt attempt = new ProfileGoalOnboardingAttempt();
        attempt.setId(proposal.attemptId());
        attempt.setUserId(userId);
        attempt.setAge(request.age());
        attempt.setCurrentWeight(request.currentWeight());
        attempt.setWeightUnit(request.weightUnit());
        attempt.setPrimaryGoal(request.primaryGoal());
        attempt.setGoalTargetBucket(request.goalTargetBucket());
        attempt.setStatus(OnboardingAttemptStatus.IN_PROGRESS);
        attempt.setCreatedAt(OffsetDateTime.now());
        attemptRepository.save(attempt);

        try {
            String payload = proposalMapper.toPayloadJson(
                    new OnboardingProposalMapper.ProposedProgramPayload(proposal.sessions()));

            PlanProposal planProposal = new PlanProposal();
            planProposal.setId(proposal.proposalId());
            planProposal.setAttempt(attempt);
            planProposal.setVersion(proposal.version());
            planProposal.setStatus(proposal.status());
            planProposal.setProposalPayload(payload);
            planProposal.setProvider(proposal.generatedBy().provider());
            planProposal.setModelDeployment(proposal.generatedBy().deployment());
            planProposal.setUserId(userId);
            planProposal.setCreatedAt(OffsetDateTime.now());

            proposalRepository.save(planProposal);
        } catch (Exception e) {
            logger.error("Failed to serialize proposal payload", e);
            throw new RuntimeException("Failed to persist proposal", e);
        }

        return proposal;
    }

    @Transactional(readOnly = true)
    public Optional<OnboardingAttemptResponse> getCurrentAttempt(UUID userId) {
        Optional<ProfileGoalOnboardingAttempt> attemptOpt = attemptRepository
                .findFirstByUserIdAndStatus(userId, OnboardingAttemptStatus.IN_PROGRESS);

        if (attemptOpt.isEmpty()) {
            return Optional.empty();
        }

        ProfileGoalOnboardingAttempt attempt = attemptOpt.get();

        Optional<PlanProposal> latestProposalOpt = proposalRepository
                .findFirstByAttempt_IdOrderByVersionDesc(attempt.getId());

        if (latestProposalOpt.isEmpty()) {
            return Optional.empty();
        }

        PlanProposal latestProposal = latestProposalOpt.get();
        PlanProposalResponse proposalResponse = proposalMapper.toResponse(latestProposal);

        return Optional.of(new OnboardingAttemptResponse(
                attempt.getId(),
                attempt.getStatus(),
                new com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProfileGoalSnapshot(
                        attempt.getAge(),
                        attempt.getCurrentWeight(),
                        attempt.getWeightUnit(),
                        attempt.getPrimaryGoal(),
                        attempt.getGoalTargetBucket()),
                proposalResponse));
    }

    @Transactional
    public PlanProposalResponse createRevision(UUID userId, UUID proposalId, String requestedChanges) {
        Optional<PlanProposal> parentProposalOpt = proposalRepository.findById(proposalId);
        if (parentProposalOpt.isEmpty()) {
            throw new RuntimeException("Proposal not found: " + proposalId);
        }

        PlanProposal parentProposal = parentProposalOpt.get();

        if (!parentProposal.getUserId().equals(userId)) {
            throw new RuntimeException("User does not own this proposal");
        }

        // Keep for prompt-context extension in later iterations.
        if (requestedChanges != null && !requestedChanges.isBlank()) {
            logger.debug("Revision requested for proposal {}: {}", proposalId, requestedChanges);
        }

        UUID attemptId = parentProposal.getAttempt().getId();
        OnboardingSubmissionRequest originalRequest = resolveAttemptSnapshot(userId, attemptId);

        PlanProposalResponse revised = onboardingPlanGenerator.generateInitialProposal(userId, originalRequest);

        try {
            String payload = proposalMapper.toPayloadJson(
                    new OnboardingProposalMapper.ProposedProgramPayload(revised.sessions()));

            PlanProposal newProposal = new PlanProposal();
            newProposal.setId(UUID.randomUUID());
            newProposal.setAttempt(parentProposal.getAttempt());
            newProposal.setVersion(parentProposal.getVersion() + 1);
            newProposal.setStatus(ProposalStatus.PROPOSED);
            newProposal.setProposalPayload(payload);
            newProposal.setProvider(revised.generatedBy().provider());
            newProposal.setModelDeployment(revised.generatedBy().deployment());
            newProposal.setUserId(userId);
            newProposal.setCreatedAt(OffsetDateTime.now());

            proposalRepository.save(newProposal);

            return new PlanProposalResponse(
                    attemptId,
                    newProposal.getId(),
                    newProposal.getVersion(),
                    newProposal.getStatus(),
                    revised.generatedBy(),
                    revised.sessions());

        } catch (Exception e) {
            logger.error("Failed to serialize revised proposal", e);
            throw new RuntimeException("Failed to persist revised proposal", e);
        }
    }

    @Transactional(readOnly = true)
    public OnboardingSubmissionRequest resolveAttemptSnapshot(UUID userId, UUID attemptId) {
        Optional<ProfileGoalOnboardingAttempt> attemptOpt = attemptRepository.findById(attemptId);
        if (attemptOpt.isEmpty()) {
            throw new RuntimeException("Attempt not found: " + attemptId);
        }

        ProfileGoalOnboardingAttempt attempt = attemptOpt.get();
        if (!attempt.getUserId().equals(userId)) {
            throw new RuntimeException("User does not own this attempt");
        }

        return new OnboardingSubmissionRequest(
                attempt.getAge(),
                attempt.getCurrentWeight(),
                attempt.getWeightUnit(),
                attempt.getPrimaryGoal(),
                attempt.getGoalTargetBucket());
    }

    @Transactional(readOnly = true)
    public TrackingAccessGateResponse getTrackingAccessGate(UUID userId) {
        boolean hasAcceptedAttempt = attemptRepository
                .existsByUserIdAndStatus(userId, OnboardingAttemptStatus.ACCEPTED);

        if (hasAcceptedAttempt) {
            return new TrackingAccessGateResponse(true, "ALLOWED", null);
        }

        boolean hasActiveActivation = activationRepository.existsByUserId(userId);
        if (hasActiveActivation) {
            return new TrackingAccessGateResponse(true, "ALLOWED", null);
        }

        UUID currentAttemptId = attemptRepository
                .findFirstByUserIdAndStatus(userId, OnboardingAttemptStatus.IN_PROGRESS)
                .map(ProfileGoalOnboardingAttempt::getId)
                .orElse(null);
        return new TrackingAccessGateResponse(false, "ONBOARDING_REQUIRED", currentAttemptId);
    }
}
