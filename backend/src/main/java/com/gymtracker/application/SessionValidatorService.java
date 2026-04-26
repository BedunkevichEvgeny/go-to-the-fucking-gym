package com.gymtracker.application;

import com.gymtracker.api.dto.CardioLapInput;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.SessionFeelingsInput;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionValidatorService {

    private final ProgramExerciseTargetRepository programExerciseTargetRepository;

    public SessionValidatorService(ProgramExerciseTargetRepository programExerciseTargetRepository) {
        this.programExerciseTargetRepository = programExerciseTargetRepository;
    }

    public void validateProgramSessionNotModifiable(UUID programSessionId, List<ExerciseEntryInput> entries) {
        List<String> targets = programExerciseTargetRepository.findByProgramSession_IdOrderBySortOrderAsc(programSessionId)
                .stream()
                .map(target -> target.getExerciseName().toLowerCase())
                .toList();
        List<String> submitted = entries.stream().map(entry -> entry.exerciseName().toLowerCase()).toList();
        if (!targets.equals(submitted)) {
            throw new ForbiddenException("Program exercises must match the prescribed order and names");
        }
    }

    public void validateBodyweightSet(StrengthSetInput set) {
        if (Boolean.TRUE.equals(set.isBodyWeight()) && set.weightValue() != null) {
            throw new ValidationException("Bodyweight sets must not include a weight value");
        }
        if (!Boolean.TRUE.equals(set.isBodyWeight()) && set.weightValue() == null) {
            throw new ValidationException("Non-bodyweight sets require a weight value");
        }
    }

    public void validateCardioLap(CardioLapInput lap) {
        if (lap.durationSeconds() == null || lap.durationSeconds() < 1) {
            throw new ValidationException("Cardio lap duration must be at least 1 second");
        }
    }

    public void validateExerciseEntry(ExerciseEntryInput entry) {
        if (entry.exerciseType() == ExerciseType.CARDIO) {
            if (entry.cardioLaps() == null || entry.cardioLaps().isEmpty()) {
                throw new ValidationException("Cardio exercises require at least one lap");
            }
            entry.cardioLaps().forEach(this::validateCardioLap);
            return;
        }
        if (entry.sets() == null || entry.sets().isEmpty()) {
            throw new ValidationException("Strength and bodyweight exercises require at least one set");
        }
        entry.sets().forEach(this::validateBodyweightSet);
    }

    public void validateSessionFeelings(SessionFeelingsInput feelings) {
        if (feelings == null || feelings.rating() == null || feelings.rating() < 1 || feelings.rating() > 10) {
            throw new ValidationException("Session feeling rating must be between 1 and 10");
        }
    }
}

