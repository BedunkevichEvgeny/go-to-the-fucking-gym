package com.gymtracker.infrastructure.mapper;

import com.gymtracker.api.dto.CardioLapView;
import com.gymtracker.api.dto.ExerciseView;
import com.gymtracker.api.dto.ExerciseEntryView;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.ProgramExerciseTargetView;
import com.gymtracker.api.dto.ProgramSessionView;
import com.gymtracker.api.dto.ProgressionPoint;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.SessionHistoryItem;
import com.gymtracker.api.dto.StrengthSetView;
import com.gymtracker.domain.CardioLap;
import com.gymtracker.domain.Exercise;
import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.ProgramExerciseTarget;
import com.gymtracker.domain.ProgramSession;
import com.gymtracker.domain.ProgressionMetricType;
import com.gymtracker.domain.StrengthSet;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public ProgramSessionView toDto(ProgramSession session, List<ProgramExerciseTarget> targets) {
        return new ProgramSessionView(
                session.getId(),
                session.getSequenceNumber(),
                session.getName(),
                targets.stream()
                        .sorted(Comparator.comparingInt(ProgramExerciseTarget::getSortOrder))
                        .map(target -> new ProgramExerciseTargetView(
                                target.getExerciseName(),
                                target.getExerciseType(),
                                target.getTargetSets(),
                                target.getTargetReps(),
                                target.getTargetWeight(),
                                target.getTargetWeightUnit(),
                                target.getTargetDurationSeconds(),
                                target.getTargetDistance(),
                                target.getTargetDistanceUnit()))
                        .toList());
    }

    public LoggedSessionDetail toDetailDto(LoggedSession loggedSession) {
        String suggestion = loggedSession.getAiSuggestion() == null
                ? null
                : loggedSession.getAiSuggestion().getSuggestion();
        return new LoggedSessionDetail(
                loggedSession.getId(),
                loggedSession.getSessionType(),
                loggedSession.getProgramSessionId(),
                loggedSession.getSessionDate(),
                loggedSession.getName(),
                loggedSession.getNotes(),
                loggedSession.getTotalDurationSeconds(),
                loggedSession.getFeelings() == null ? null : new SessionFeelingsInput(
                        loggedSession.getFeelings().getRating(),
                        loggedSession.getFeelings().getComment()),
                loggedSession.getExerciseEntries().stream()
                        .sorted(Comparator.comparingInt(ExerciseEntry::getSortOrder))
                        .map(entry -> new ExerciseEntryView(
                                entry.getExerciseId(),
                                entry.getCustomExerciseName(),
                                entry.getExerciseNameSnapshot(),
                                entry.getExerciseType(),
                                entry.getStrengthSets().stream()
                                        .sorted(Comparator.comparingInt(StrengthSet::getSetOrder))
                                        .map(set -> new StrengthSetView(
                                                set.getSetOrder(),
                                                set.getReps(),
                                                set.getWeightValue(),
                                                set.getWeightUnit(),
                                                set.isBodyWeight(),
                                                set.getDurationSeconds(),
                                                set.getRestSeconds()))
                                        .toList(),
                                entry.getCardioLaps().stream()
                                        .sorted(Comparator.comparingInt(CardioLap::getLapOrder))
                                        .map(lap -> new CardioLapView(
                                                lap.getLapOrder(),
                                                lap.getDurationSeconds(),
                                                lap.getDistanceValue(),
                                                lap.getDistanceUnit()))
                                        .toList()))
                        .toList(),
                suggestion);
    }

    public SessionHistoryItem toHistoryItem(LoggedSession loggedSession) {
        return new SessionHistoryItem(
                loggedSession.getId(),
                loggedSession.getSessionDate(),
                loggedSession.getSessionType(),
                loggedSession.getExerciseEntries().size(),
                loggedSession.getTotalDurationSeconds(),
                loggedSession.getName());
    }

    public ProgressionPoint toProgressionPoint(UUID sessionId, java.time.LocalDate sessionDate, ProgressionMetricType metricType, double metricValue) {
        return new ProgressionPoint(sessionId, sessionDate, metricType, metricValue);
    }

    public ExerciseView toExerciseView(Exercise exercise) {
        return new ExerciseView(exercise.getId(), exercise.getName(), exercise.getCategory(), exercise.getType(), exercise.getDescription());
    }
}

