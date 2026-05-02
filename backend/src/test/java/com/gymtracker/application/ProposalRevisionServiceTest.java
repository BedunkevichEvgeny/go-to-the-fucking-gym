package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposalRejectRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedSession;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.WeightUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalRevisionServiceTest {

    @Mock
    private PlanProposalService planProposalService;

    @Mock
    private ProposalFeedbackService proposalFeedbackService;

    @InjectMocks
    private ProposalRevisionService service;

    @Test
    void createsRevisedProposalWithNextVersion() {
        UUID userId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();

        PlanProposalResponse revised = new PlanProposalResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2,
                ProposalStatus.PROPOSED,
                new com.gymtracker.api.dto.ProfileGoalOnboardingDtos.GeneratedBy(ProposalProvider.AZURE_OPENAI, "gpt-35-turbo"),
                List.of(new ProposedSession(1, "Revised Session", List.of())));

        when(planProposalService.createRevision(any(), any(), any())).thenReturn(revised);

        PlanProposalResponse response = service.rejectAndRevise(
                userId,
                proposalId,
                new ProposalRejectRequest("Reduce leg volume"));

        verify(proposalFeedbackService).storeFeedback(userId, proposalId, "Reduce leg volume");
        verify(planProposalService).createRevision(any(), any(), any());
        assertThat(response.version()).isEqualTo(2);
    }

    @Test
    void keepsRevisionLinkedToSameAttemptContext() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        OnboardingSubmissionRequest snapshot = new OnboardingSubmissionRequest(
                29,
                new BigDecimal("81.0"),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null);

        when(planProposalService.resolveAttemptSnapshot(userId, attemptId)).thenReturn(snapshot);

        OnboardingSubmissionRequest resolved = service.resolveAttemptSnapshot(userId, attemptId);

        assertThat(resolved.primaryGoal()).isEqualTo(OnboardingPrimaryGoal.STRENGTH);
        assertThat(resolved.age()).isEqualTo(29);
    }
}

