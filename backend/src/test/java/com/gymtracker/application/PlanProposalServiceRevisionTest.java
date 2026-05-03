package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.GeneratedBy;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedSession;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.ProfileGoalOnboardingAttempt;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.ai.OnboardingPlanGenerator;
import com.gymtracker.infrastructure.mapper.OnboardingProposalMapper;
import com.gymtracker.infrastructure.repository.AcceptedProgramActivationRepository;
import com.gymtracker.infrastructure.repository.PlanProposalRepository;
import com.gymtracker.infrastructure.repository.ProfileGoalOnboardingAttemptRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// ===== T071: Assert requestedChanges routing in PlanProposalService.createRevision() =====

/**
 * Unit tests verifying that {@link PlanProposalService#createRevision} routes to
 * {@link OnboardingPlanGenerator#generateRevision} when {@code requestedChanges} is
 * non-blank, and to {@link OnboardingPlanGenerator#generateInitialProposal} otherwise.
 */
@ExtendWith(MockitoExtension.class)
class PlanProposalServiceRevisionTest {

    @Mock private OnboardingValidationService validationService;
    @Mock private OnboardingPlanGenerator onboardingPlanGenerator;
    @Mock private ProfileGoalOnboardingAttemptRepository attemptRepository;
    @Mock private PlanProposalRepository proposalRepository;
    @Mock private AcceptedProgramActivationRepository activationRepository;
    @Mock private OnboardingProposalMapper proposalMapper;

    @InjectMocks
    private PlanProposalService service;

    private UUID userId;
    private UUID proposalId;
    private UUID attemptId;
    private PlanProposal parentProposal;
    private ProfileGoalOnboardingAttempt attempt;
    private OnboardingSubmissionRequest originalRequest;
    private PlanProposalResponse dummyResponse;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        proposalId = UUID.randomUUID();
        attemptId = UUID.randomUUID();

        originalRequest = new OnboardingSubmissionRequest(
                30, new BigDecimal("80.0"), WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH, null);

        attempt = new ProfileGoalOnboardingAttempt();
        attempt.setId(attemptId);
        attempt.setUserId(userId);
        attempt.setAge(originalRequest.age());
        attempt.setCurrentWeight(originalRequest.currentWeight());
        attempt.setWeightUnit(originalRequest.weightUnit());
        attempt.setPrimaryGoal(originalRequest.primaryGoal());
        attempt.setCreatedAt(OffsetDateTime.now());

        parentProposal = new PlanProposal();
        parentProposal.setId(proposalId);
        parentProposal.setUserId(userId);
        parentProposal.setAttempt(attempt);
        parentProposal.setVersion(1);
        parentProposal.setStatus(ProposalStatus.PROPOSED);

        dummyResponse = new PlanProposalResponse(
                attemptId,
                UUID.randomUUID(),
                1,
                ProposalStatus.PROPOSED,
                new GeneratedBy(ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(new ProposedSession(1, "Session A", List.of())));

        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(parentProposal));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(proposalMapper.toPayloadJson(any())).thenReturn("{}");
    }

    // ── T071-1: non-blank requestedChanges → generateRevision() ─────────────

    @Test
    void createRevision_WithNonBlankFeedback_CallsGenerateRevision() {
        String feedback = "Add more cardio, remove leg day";
        when(onboardingPlanGenerator.generateRevision(eq(userId), any(), eq(feedback)))
                .thenReturn(dummyResponse);

        service.createRevision(userId, proposalId, feedback);

        verify(onboardingPlanGenerator).generateRevision(eq(userId), any(), eq(feedback));
        verify(onboardingPlanGenerator, never()).generateInitialProposal(any(), any());
    }

    @Test
    void createRevision_WithNonBlankFeedback_PassesFeedbackStringToGenerator() {
        String feedback = "Less volume on upper body";
        when(onboardingPlanGenerator.generateRevision(any(), any(), eq(feedback)))
                .thenReturn(dummyResponse);

        service.createRevision(userId, proposalId, feedback);

        verify(onboardingPlanGenerator).generateRevision(any(), any(), eq(feedback));
    }

    // ── T071-2: null or blank requestedChanges → generateInitialProposal() ──

    @Test
    void createRevision_WithNullFeedback_CallsGenerateInitialProposal() {
        when(onboardingPlanGenerator.generateInitialProposal(eq(userId), any()))
                .thenReturn(dummyResponse);

        service.createRevision(userId, proposalId, null);

        verify(onboardingPlanGenerator).generateInitialProposal(eq(userId), any());
        verify(onboardingPlanGenerator, never()).generateRevision(any(), any(), any());
    }

    @Test
    void createRevision_WithBlankFeedback_CallsGenerateInitialProposal() {
        when(onboardingPlanGenerator.generateInitialProposal(eq(userId), any()))
                .thenReturn(dummyResponse);

        service.createRevision(userId, proposalId, "   ");

        verify(onboardingPlanGenerator).generateInitialProposal(eq(userId), any());
        verify(onboardingPlanGenerator, never()).generateRevision(any(), any(), any());
    }

    @Test
    void createRevision_WithEmptyFeedback_CallsGenerateInitialProposal() {
        when(onboardingPlanGenerator.generateInitialProposal(eq(userId), any()))
                .thenReturn(dummyResponse);

        service.createRevision(userId, proposalId, "");

        verify(onboardingPlanGenerator).generateInitialProposal(eq(userId), any());
        verify(onboardingPlanGenerator, never()).generateRevision(any(), any(), any());
    }

    // ── T071-3: return value carries correct version increment ───────────────

    @Test
    void createRevision_WithFeedback_ReturnsVersionIncrementedByOne() {
        String feedback = "More compound lifts";
        when(onboardingPlanGenerator.generateRevision(any(), any(), any()))
                .thenReturn(dummyResponse);

        PlanProposalResponse result = service.createRevision(userId, proposalId, feedback);

        assertThat(result.version()).isEqualTo(parentProposal.getVersion() + 1);
    }
}

