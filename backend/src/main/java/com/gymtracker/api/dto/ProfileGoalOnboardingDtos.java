package com.gymtracker.api.dto;

import com.gymtracker.domain.ExerciseType;
import com.gymtracker.domain.DistanceUnit;
import com.gymtracker.domain.OnboardingEnums.GoalTargetBucket;
import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
import com.gymtracker.domain.WeightUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ProfileGoalOnboardingDtos {

    private ProfileGoalOnboardingDtos() {
    }

    public record OnboardingSubmissionRequest(
            @Min(13) @Max(100) int age,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal currentWeight,
            @NotNull WeightUnit weightUnit,
            @NotNull OnboardingPrimaryGoal primaryGoal,
            GoalTargetBucket goalTargetBucket
    ) {
    }

    public record ProposalRejectRequest(@NotBlank @Size(max = 2000) String requestedChanges) {
    }

    public record ProfileGoalSnapshot(
            int age,
            BigDecimal currentWeight,
            WeightUnit weightUnit,
            OnboardingPrimaryGoal primaryGoal,
            GoalTargetBucket goalTargetBucket
    ) {
    }

    public record GeneratedBy(ProposalProvider provider, String deployment) {
    }

    public record ProposedExerciseTarget(
            String exerciseName,
            ExerciseType exerciseType,
            Integer targetSets,
            Integer targetReps,
            BigDecimal targetWeight,
            WeightUnit targetWeightUnit,
            Integer targetDurationSeconds,
            BigDecimal targetDistance,
            DistanceUnit targetDistanceUnit
    ) {
    }

    public record ProposedSession(int sequenceNumber, String name, List<ProposedExerciseTarget> exercises) {
    }

    public record PlanProposalResponse(
            UUID attemptId,
            UUID proposalId,
            int version,
            ProposalStatus status,
            GeneratedBy generatedBy,
            List<ProposedSession> sessions
    ) {
    }

    public record OnboardingAttemptResponse(
            UUID attemptId,
            OnboardingAttemptStatus status,
            ProfileGoalSnapshot profileGoal,
            PlanProposalResponse latestProposal
    ) {
    }

    public record ProposalAcceptanceResponse(
            UUID proposalId,
            UUID activatedProgramId,
            UUID replacedProgramId,
            OffsetDateTime activatedAt
    ) {
    }

    public record TrackingAccessGateResponse(
            boolean canAccessProgramTracking,
            String reasonCode,
            UUID currentAttemptId
    ) {
    }
}


