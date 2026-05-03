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
import com.gymtracker.infrastructure.ai.dto.ExerciseDto;
import com.gymtracker.infrastructure.ai.dto.OnboardingPlanDto;
import com.gymtracker.infrastructure.ai.dto.SessionDto;
import com.gymtracker.infrastructure.config.AzureOpenAiOnboardingProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OnboardingPlanGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingPlanGenerator.class);

    private final AzureOpenAiOnboardingProperties properties;
    private final LangChainSessionProcessor langChainProcessor;

    public OnboardingPlanGenerator(
        AzureOpenAiOnboardingProperties properties,
        LangChainSessionProcessor langChainProcessor) {
        this.properties = properties;
        this.langChainProcessor = langChainProcessor;
    }

    public PlanProposalResponse generateInitialProposal(UUID userId, OnboardingSubmissionRequest request) {
        UUID attemptId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();

        String prompt = buildPrompt(request);
        logger.debug("Generating proposal with prompt: {}", prompt);

        try {
            OnboardingPlanDto planDto = langChainProcessor.processOnboarding(userId.toString(), prompt);

            if (planDto == null || planDto.sessions() == null || planDto.sessions().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI returned no usable sessions for onboarding proposal");
            }

            List<ProposedSession> sessions = mapSessions(planDto, request);
            if (sessions.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI returned no usable sessions for onboarding proposal");
            }

            return new PlanProposalResponse(
                    attemptId,
                    proposalId,
                    1,
                    ProposalStatus.PROPOSED,
                    new GeneratedBy(ProposalProvider.AZURE_OPENAI, properties.getDeployment()),
                    sessions);
        } catch (ResponseStatusException responseStatusException) {
            throw responseStatusException;
        } catch (Exception exception) {
            logger.error("Failed to generate onboarding proposal from AI", exception);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to generate onboarding plan from AI service",
                    exception);
        }
    }

    // package-private for testing
    String buildPrompt(OnboardingSubmissionRequest request) {
        return String.format("""
                Create a personalized 3-4 week fitness plan for someone with these characteristics:
                - Age: %d years
                - Current Weight: %.1f %s
                - Primary Goal: %s
                - Experience Level: beginner
                
                Generate a structured plan with 3-4 sessions per week. Each session should include:
                - Mix of strength training, cardio, and mobility/bodyweight work
                - Realistic exercise selections based on the person's profile
                - Moderate intensity appropriate for the age and current fitness
                
                IMPORTANT: Generate DIFFERENT exercises based on their profile. Do NOT use generic templates like "Back Squat" and "Treadmill Run".
                
                Return ONLY valid JSON (no markdown, no code blocks) with this exact structure:
                {
                  "sessions": [
                    {
                      "sequenceNumber": 1,
                      "name": "Session Name",
                      "exercises": [
                        {
                          "name": "Exercise Name",
                          "type": "STRENGTH|BODYWEIGHT|CARDIO",
                          "targetSets": 3,
                          "targetReps": 8,
                          "targetWeight": 50.0,
                          "weightUnit": "KG",
                          "durationSeconds": null
                        }
                      ]
                    }
                  ]
                }
                """,
            request.age(),
            request.currentWeight().doubleValue(),
            request.weightUnit(),
            request.primaryGoal());
    }

    private List<ProposedSession> mapSessions(OnboardingPlanDto planDto, OnboardingSubmissionRequest request) {
        List<ProposedSession> sessions = new ArrayList<>();
        for (SessionDto sessionDto : planDto.sessions()) {
            if (sessionDto == null) {
                continue;
            }
            int sequenceNumber = sessionDto.sequenceNumber();
            String sessionName = sessionDto.name() == null ? "" : sessionDto.name().trim();
            List<ProposedExerciseTarget> exercises = mapExercises(sessionDto.exercises(), request);

            if (sequenceNumber > 0 && !sessionName.isEmpty() && !exercises.isEmpty()) {
                sessions.add(new ProposedSession(sequenceNumber, sessionName, exercises));
            }
        }
        return sessions;
    }

    private List<ProposedExerciseTarget> mapExercises(List<ExerciseDto> exerciseDtos,
                                                       OnboardingSubmissionRequest request) {
        List<ProposedExerciseTarget> targets = new ArrayList<>();
        if (exerciseDtos == null) {
            return targets;
        }
        WeightUnit weightUnit = request.weightUnit() == null ? WeightUnit.KG : request.weightUnit();
        for (ExerciseDto dto : exerciseDtos) {
            if (dto == null || dto.name() == null || dto.name().isBlank()) {
                continue;
            }
            ExerciseType type = dto.type() != null ? dto.type() : ExerciseType.STRENGTH;
            targets.add(new ProposedExerciseTarget(
                    dto.name(),
                    type,
                    dto.targetSets(),
                    dto.targetReps(),
                    dto.targetWeight(),
                    weightUnit,
                    dto.durationSeconds(),
                    null,
                    null));
        }
        return targets;
    }
}
