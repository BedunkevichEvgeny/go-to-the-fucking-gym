package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedSession;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.ai.OnboardingPlanGenerator;
import com.gymtracker.infrastructure.mapper.OnboardingProposalMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanProposalServiceTest {

    @Mock
    private OnboardingValidationService validationService;

    @Mock
    private OnboardingPlanGenerator onboardingPlanGenerator;

    @Mock
    private OnboardingProposalMapper onboardingProposalMapper;

    @InjectMocks
    private PlanProposalService service;

    @Test
    void createsInitialProposalUsingAiGenerator() {
        UUID userId = UUID.randomUUID();
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                30,
                new BigDecimal("84.2"),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null);

        PlanProposalResponse generated = new PlanProposalResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                com.gymtracker.domain.OnboardingEnums.ProposalStatus.PROPOSED,
                new com.gymtracker.api.dto.ProfileGoalOnboardingDtos.GeneratedBy(ProposalProvider.AZURE_OPENAI, "test"),
                List.of(new ProposedSession(1, "Session A", List.of())));

        when(onboardingPlanGenerator.generateInitialProposal(any())).thenReturn(generated);

        PlanProposalResponse response = service.createInitialProposal(userId, request);

        verify(validationService).validate(request);
        verify(onboardingPlanGenerator).generateInitialProposal(any());
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.generatedBy().provider()).isEqualTo(ProposalProvider.AZURE_OPENAI);
    }
}

