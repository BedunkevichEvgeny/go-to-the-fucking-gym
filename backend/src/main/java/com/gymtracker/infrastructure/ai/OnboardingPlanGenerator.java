package com.gymtracker.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            String llmResponse = langChainProcessor.process(
                    new SessionSummaryDTO(
                            userId,
                            UUID.randomUUID(),
                            null,
                            null,
                            0,
                            null,
                            new SessionSummaryDTO.UserPreferences(request.weightUnit().toString()),
                            List.of()));

            List<ProposedSession> sessions = parseLlmResponse(llmResponse, request);
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

    private String buildPrompt(OnboardingSubmissionRequest request) {
        return String.format("""
                Create a personalized 3-4 week fitness plan for someone with these characteristics:
                - Age: %d years
                - Current Weight: %.1f %s
                - Primary Goal: %s
                - Experience Level: beginner
                
                Generate a structured plan with 3-4 sessions per week. Each session should include:
                - Mix of strength training, cardio, and flexibility work
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
                          "type": "STRENGTH|CARDIO|FLEXIBILITY",
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

    private List<ProposedSession> parseLlmResponse(String response, OnboardingSubmissionRequest request) {
        if (response == null || response.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an empty response");
        }

        try {
            String jsonPayload = extractJsonPayload(response);
            JsonNode root = objectMapper.readTree(jsonPayload);
            List<ProposedSession> sessions = new ArrayList<>();

            JsonNode sessionsNode = root.get("sessions");
            if (sessionsNode == null || !sessionsNode.isArray()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI response JSON does not contain a valid 'sessions' array");
            }

            for (JsonNode sessionNode : sessionsNode) {
                int sequenceNumber = sessionNode.path("sequenceNumber").asInt(0);
                String sessionName = sessionNode.path("name").asText("").trim();
                List<ProposedExerciseTarget> exercises = new ArrayList<>();

                JsonNode exercisesNode = sessionNode.get("exercises");
                if (exercisesNode != null && exercisesNode.isArray()) {
                    for (JsonNode exerciseNode : exercisesNode) {
                        ProposedExerciseTarget target = parseExercise(exerciseNode, request);
                        if (target != null) {
                            exercises.add(target);
                        }
                    }
                }

                if (sequenceNumber > 0 && !sessionName.isEmpty() && !exercises.isEmpty()) {
                    sessions.add(new ProposedSession(sequenceNumber, sessionName, exercises));
                }
            }

            return sessions;
        } catch (ResponseStatusException responseStatusException) {
            throw responseStatusException;
        } catch (Exception exception) {
            logger.error("Failed to parse AI response as onboarding proposal JSON", exception);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI returned malformed JSON for onboarding proposal",
                    exception);
        }
    }

    private String extractJsonPayload(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI response is not valid JSON: " + trimmed);
    }

    private ProposedExerciseTarget parseExercise(JsonNode exerciseNode, OnboardingSubmissionRequest request) {
        try {
            String exerciseName = exerciseNode.path("name").asText(exerciseNode.path("exerciseName").asText(""));
            String typeStr = exerciseNode.path("type").asText(exerciseNode.path("exerciseType").asText("STRENGTH"));
            ExerciseType type = ExerciseType.valueOf(typeStr);

            if (exerciseName == null || exerciseName.isBlank()) {
                return null;
            }

            Integer targetSets = null;
            Integer targetReps = null;
            BigDecimal targetWeight = null;
            Integer durationSeconds = null;

            if (exerciseNode.has("targetSets") && !exerciseNode.get("targetSets").isNull()) {
                targetSets = exerciseNode.get("targetSets").asInt();
            }
            if (exerciseNode.has("targetReps") && !exerciseNode.get("targetReps").isNull()) {
                targetReps = exerciseNode.get("targetReps").asInt();
            }
            if (exerciseNode.has("targetWeight") && !exerciseNode.get("targetWeight").isNull()) {
                targetWeight = BigDecimal.valueOf(exerciseNode.get("targetWeight").asDouble());
            }
            if (exerciseNode.has("durationSeconds") && !exerciseNode.get("durationSeconds").isNull()) {
                durationSeconds = exerciseNode.get("durationSeconds").asInt();
            }

            WeightUnit weightUnit = request.weightUnit() == null ? WeightUnit.KG : request.weightUnit();

            return new ProposedExerciseTarget(
                    exerciseName,
                    type,
                    targetSets,
                    targetReps,
                    targetWeight,
                    weightUnit,
                    durationSeconds,
                    null,
                    null);

        } catch (Exception exception) {
            logger.warn("Failed to parse exercise node", exception);
            return null;
        }
    }
}
