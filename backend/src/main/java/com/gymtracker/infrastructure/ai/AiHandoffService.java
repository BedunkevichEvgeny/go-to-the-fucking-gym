package com.gymtracker.infrastructure.ai;

import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.ProgramExerciseTarget;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.StrengthSet;
import com.gymtracker.domain.User;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import com.gymtracker.infrastructure.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiHandoffService {

    private static final Logger log = LoggerFactory.getLogger(AiHandoffService.class);

    private final LangChainSessionProcessor processor;
    private final ProgramExerciseTargetRepository programExerciseTargetRepository;
    private final UserRepository userRepository;

    public AiHandoffService(
            LangChainSessionProcessor processor,
            ProgramExerciseTargetRepository programExerciseTargetRepository,
            UserRepository userRepository
    ) {
        this.processor = processor;
        this.programExerciseTargetRepository = programExerciseTargetRepository;
        this.userRepository = userRepository;
    }

    public void enqueueSessionForAiAnalysis(UUID userId, LoggedSession session) {
        if (session == null || session.getSessionType() != SessionType.PROGRAM) {
            return;
        }

        SessionSummaryDTO summary = buildSessionSummary(userId, session);
        try {
            processor.processAsync(summary).whenComplete((response, exception) -> {
                if (exception != null) {
                    log.error("AI handoff failed for session {}", summary.sessionId(), exception);
                    return;
                }
                log.info("AI handoff finished for session {} with response: {}", summary.sessionId(), response);
            });
        } catch (Exception exception) {
            log.error("AI handoff failed for session {}", session.getId(), exception);
        }
    }

    public SessionSummaryDTO buildSessionSummary(UUID userId, LoggedSession session) {
        Map<String, ProgramExerciseTarget> targetsByExercise = loadTargetsByExercise(session);
        List<SessionSummaryDTO.ExerciseSummary> exerciseSummaries = session.getExerciseEntries().stream()
                .map(entry -> buildExerciseSummary(entry, targetsByExercise.get(normalizeExerciseName(entry.getExerciseNameSnapshot()))))
                .toList();

        User user = userRepository.findById(userId).orElse(null);
        return new SessionSummaryDTO(
                userId,
                session.getId(),
                session.getSessionType(),
                session.getSessionDate(),
                session.getTotalDurationSeconds(),
                new SessionSummaryDTO.FeelingsSummary(
                        session.getFeelings() == null ? null : session.getFeelings().getRating(),
                        session.getFeelings() == null ? null : session.getFeelings().getComment()),
                new SessionSummaryDTO.UserPreferences(
                        user == null || user.getPreferredWeightUnit() == null ? "UNKNOWN" : user.getPreferredWeightUnit().name()),
                exerciseSummaries);
    }

    private Map<String, ProgramExerciseTarget> loadTargetsByExercise(LoggedSession session) {
        if (session.getSessionType() != SessionType.PROGRAM || session.getProgramSessionId() == null) {
            return Map.of();
        }

        List<ProgramExerciseTarget> targets =
                programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(session.getProgramSessionId());
        Map<String, ProgramExerciseTarget> result = new HashMap<>();
        for (ProgramExerciseTarget target : targets) {
            result.put(normalizeExerciseName(target.getExerciseName()), target);
        }
        return result;
    }

    private SessionSummaryDTO.ExerciseSummary buildExerciseSummary(ExerciseEntry entry, ProgramExerciseTarget target) {
        int totalReps = entry.getStrengthSets().stream().mapToInt(StrengthSet::getReps).sum();
        BigDecimal maxWeight = entry.getStrengthSets().stream()
                .map(StrengthSet::getWeightValue)
                .filter(value -> value != null)
                .max(BigDecimal::compareTo)
                .orElse(null);
        int totalDurationSeconds = entry.getCardioLaps().stream().mapToInt(lap -> lap.getDurationSeconds()).sum();
        BigDecimal totalDistance = entry.getCardioLaps().stream()
                .map(lap -> lap.getDistanceValue())
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SessionSummaryDTO.TargetPerformance targetPerformance = target == null
                ? null
                : new SessionSummaryDTO.TargetPerformance(
                        target.getTargetSets(),
                        target.getTargetReps(),
                        target.getTargetWeight(),
                        target.getTargetDurationSeconds(),
                        target.getTargetDistance());

        return new SessionSummaryDTO.ExerciseSummary(
                entry.getExerciseNameSnapshot(),
                entry.getExerciseType(),
                new SessionSummaryDTO.ActualPerformance(
                        entry.getStrengthSets().size(),
                        totalReps,
                        maxWeight,
                        totalDurationSeconds == 0 ? null : totalDurationSeconds,
                        BigDecimal.ZERO.compareTo(totalDistance) == 0 ? null : totalDistance),
                targetPerformance);
    }

    private String normalizeExerciseName(String exerciseName) {
        return exerciseName == null ? "" : exerciseName.trim().toLowerCase(Locale.ROOT);
    }
}

