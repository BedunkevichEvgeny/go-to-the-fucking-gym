package com.gymtracker.infrastructure.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.GeneratedBy;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedExerciseTarget;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposedSession;
import com.gymtracker.domain.PlanProposal;
import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingProposalMapper {

    private final ObjectMapper objectMapper;

    public String toPayloadJson(ProposedProgramPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize onboarding proposal payload", e);
        }
    }

    public ProposedProgramPayload fromPayloadJson(String payload) {
        try {
            return objectMapper.readValue(payload, ProposedProgramPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialize onboarding proposal payload", e);
        }
    }

    public PlanProposalResponse toResponse(PlanProposal proposal) {
        ProposedProgramPayload payload = fromPayloadJson(proposal.getProposalPayload());
        ProposalProvider provider = proposal.getProvider() == null ? ProposalProvider.AZURE_OPENAI : proposal.getProvider();
        return new PlanProposalResponse(
                proposal.getAttempt().getId(),
                proposal.getId(),
                proposal.getVersion(),
                proposal.getStatus(),
                new GeneratedBy(provider, proposal.getModelDeployment()),
                payload.sessions());
    }

    public record ProposedProgramPayload(List<ProposedSession> sessions) {
    }

    public record DraftSession(int sequenceNumber, String name, List<DraftExerciseTarget> exercises) {
    }

    public record DraftExerciseTarget(
            String exerciseName,
            com.gymtracker.domain.ExerciseType exerciseType,
            Integer targetSets,
            Integer targetReps,
            java.math.BigDecimal targetWeight,
            com.gymtracker.domain.WeightUnit targetWeightUnit,
            Integer targetDurationSeconds,
            java.math.BigDecimal targetDistance,
            com.gymtracker.domain.DistanceUnit targetDistanceUnit
    ) {
    }

    public ProposedProgramPayload fromDraft(List<DraftSession> sessions) {
        List<ProposedSession> mappedSessions = sessions.stream()
                .map(session -> new ProposedSession(
                        session.sequenceNumber(),
                        session.name(),
                        session.exercises().stream()
                                .map(exercise -> new ProposedExerciseTarget(
                                        exercise.exerciseName(),
                                        exercise.exerciseType(),
                                        exercise.targetSets(),
                                        exercise.targetReps(),
                                        exercise.targetWeight(),
                                        exercise.targetWeightUnit(),
                                        exercise.targetDurationSeconds(),
                                        exercise.targetDistance(),
                                        exercise.targetDistanceUnit()))
                                .toList()))
                .toList();
        return new ProposedProgramPayload(mappedSessions);
    }
}

