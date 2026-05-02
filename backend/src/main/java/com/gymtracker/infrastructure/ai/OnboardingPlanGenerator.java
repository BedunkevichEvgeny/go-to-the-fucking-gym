package com.gymtracker.infrastructure.ai;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.GeneratedBy;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedExerciseTarget;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedSession;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.WeightUnit;
import com.gymtracker.infrastructure.config.AzureOpenAiOnboardingProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingPlanGenerator {

    private final AzureOpenAiOnboardingProperties properties;

    public OnboardingPlanGenerator(AzureOpenAiOnboardingProperties properties) {
        this.properties = properties;
    }

    public PlanProposalResponse generateInitialProposal(UUID userId, OnboardingSubmissionRequest request) {
        UUID attemptId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();

        ProposedExerciseTarget squat = new ProposedExerciseTarget(
                "Back Squat",
                ExerciseType.STRENGTH,
                4,
                6,
                new BigDecimal("70"),
                request.weightUnit() == null ? WeightUnit.KG : request.weightUnit(),
                null,
                null,
                null);

        ProposedExerciseTarget run = new ProposedExerciseTarget(
                "Treadmill Run",
                ExerciseType.CARDIO,
                null,
                null,
                null,
                null,
                900,
                null,
                null);

        List<ProposedSession> sessions = List.of(
                new ProposedSession(1, "Strength Foundation", List.of(squat)),
                new ProposedSession(2, "Cardio Builder", List.of(run)));

        return new PlanProposalResponse(
                attemptId,
                proposalId,
                1,
                ProposalStatus.PROPOSED,
                new GeneratedBy(ProposalProvider.AZURE_OPENAI, properties.getDeployment()),
                sessions);
    }
}

