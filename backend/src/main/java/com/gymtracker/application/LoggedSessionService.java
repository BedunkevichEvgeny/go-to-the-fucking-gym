package com.gymtracker.application;

import com.gymtracker.api.dto.CardioLapInput;
import com.gymtracker.api.dto.ExerciseEntryInput;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.StrengthSetInput;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.CardioLap;
import com.gymtracker.domain.Exercise;
import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.SessionFeelings;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.StrengthSet;
import com.gymtracker.infrastructure.ai.AiHandoffService;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoggedSessionService {

    private static final Logger log = LoggerFactory.getLogger(LoggedSessionService.class);

    private final LoggedSessionRepository loggedSessionRepository;
    private final ProgramSessionService programSessionService;
    private final SessionValidatorService sessionValidatorService;
    private final ExerciseLibraryService exerciseLibraryService;
    private final DtoMapper dtoMapper;
    private final AiHandoffService aiHandoffService;

    public LoggedSessionService(
            LoggedSessionRepository loggedSessionRepository,
            ProgramSessionService programSessionService,
            SessionValidatorService sessionValidatorService,
            ExerciseLibraryService exerciseLibraryService,
            DtoMapper dtoMapper,
            AiHandoffService aiHandoffService
    ) {
        this.loggedSessionRepository = loggedSessionRepository;
        this.programSessionService = programSessionService;
        this.sessionValidatorService = sessionValidatorService;
        this.exerciseLibraryService = exerciseLibraryService;
        this.dtoMapper = dtoMapper;
        this.aiHandoffService = aiHandoffService;
    }

    @Transactional
    public LoggedSessionDetail saveLoggedSession(UUID userId, LoggedSessionCreateRequest request) {
        sessionValidatorService.validateSessionFeelings(request.feelings());
        if (request.exerciseEntries() == null || request.exerciseEntries().isEmpty()) {
            throw new ValidationException("Logged session must include at least one exercise entry");
        }
        request.exerciseEntries().forEach(sessionValidatorService::validateExerciseEntry);

        if (request.sessionType() == SessionType.PROGRAM) {
            saveProgramSessionGuards(userId, request);
        } else if (request.programSessionId() != null) {
            throw new ValidationException("Free sessions must not reference a programSessionId");
        }

        LoggedSession session = LoggedSession.builder()
                .userId(userId)
                .sessionType(request.sessionType())
                .programSessionId(request.programSessionId())
                .sessionDate(request.sessionDate())
                .name(request.name())
                .notes(request.notes())
                .totalDurationSeconds(request.totalDurationSeconds())
                .exerciseEntries(new ArrayList<>())
                .build();

        List<ExerciseEntry> exerciseEntries = mapExerciseEntries(session, request.exerciseEntries());
        session.setExerciseEntries(exerciseEntries);
        SessionFeelings feelings = SessionFeelings.builder()
                .session(session)
                .rating(request.feelings().rating())
                .comment(request.feelings().comment())
                .build();
        session.setFeelings(feelings);

        LoggedSession saved = loggedSessionRepository.save(session);
        if (request.sessionType() == SessionType.PROGRAM && request.programSessionId() != null) {
            programSessionService.markProgramSessionCompleted(request.programSessionId(), userId);
        }
        aiHandoffService.enqueueSessionForAiAnalysis(userId, saved);
        log.info("{} saved {} session with {} exercises", userId, request.sessionType(), request.exerciseEntries().size());
        return dtoMapper.toDetailDto(saved);
    }

    private void saveProgramSessionGuards(UUID userId, LoggedSessionCreateRequest request) {
        if (request.programSessionId() == null) {
            throw new ValidationException("Program sessions require programSessionId");
        }
        UUID expectedSessionId = programSessionService.loadNextUncompletedSession(userId)
                .map(programSessionView -> programSessionView.programSessionId())
                .orElseThrow(() -> new ForbiddenException("There is no active next program session to log"));
        if (!expectedSessionId.equals(request.programSessionId())) {
            throw new ForbiddenException("Only the next uncompleted program session can be logged");
        }
        sessionValidatorService.validateProgramSessionNotModifiable(request.programSessionId(), request.exerciseEntries());
    }

    private List<ExerciseEntry> mapExerciseEntries(LoggedSession session, List<ExerciseEntryInput> entries) {
        List<ExerciseEntry> result = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            ExerciseEntryInput entry = entries.get(index);
            Exercise libraryExercise = entry.exerciseId() == null ? null : exerciseLibraryService.getExerciseById(entry.exerciseId());
            ExerciseEntry exerciseEntry = ExerciseEntry.builder()
                    .loggedSession(session)
                    .exerciseId(libraryExercise == null ? null : libraryExercise.getId())
                    .customExerciseName(entry.customExerciseName())
                    .exerciseNameSnapshot(entry.exerciseName())
                    .exerciseType(entry.exerciseType())
                    .sortOrder(index)
                    .strengthSets(new ArrayList<>())
                    .cardioLaps(new ArrayList<>())
                    .build();

            if (entry.sets() != null) {
                int setOrder = 1;
                for (StrengthSetInput setInput : entry.sets()) {
                    StrengthSet set = StrengthSet.builder()
                            .exerciseEntry(exerciseEntry)
                            .setOrder(setOrder++)
                            .reps(setInput.reps())
                            .weightValue(setInput.weightValue())
                            .weightUnit(setInput.weightUnit())
                            .bodyWeight(Boolean.TRUE.equals(setInput.isBodyWeight()))
                            .build();
                    exerciseEntry.getStrengthSets().add(set);
                }
            }
            if (entry.cardioLaps() != null) {
                int lapOrder = 1;
                for (CardioLapInput lapInput : entry.cardioLaps()) {
                    CardioLap lap = CardioLap.builder()
                            .exerciseEntry(exerciseEntry)
                            .lapOrder(lapOrder++)
                            .durationSeconds(lapInput.durationSeconds())
                            .distanceValue(lapInput.distanceValue())
                            .distanceUnit(lapInput.distanceUnit())
                            .build();
                    exerciseEntry.getCardioLaps().add(lap);
                }
            }
            result.add(exerciseEntry);
        }
        return result;
    }
}

