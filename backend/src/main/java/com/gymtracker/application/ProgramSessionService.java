package com.gymtracker.application;

import com.gymtracker.api.dto.ProgramSessionView;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.ResourceNotFoundException;
import com.gymtracker.domain.AcceptedProgramActivation;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.ProgramSession;
import com.gymtracker.domain.WorkoutProgram;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import com.gymtracker.infrastructure.repository.ProgramSessionRepository;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramSessionService {

    private static final Logger log = LoggerFactory.getLogger(ProgramSessionService.class);

    private final WorkoutProgramRepository workoutProgramRepository;
    private final ProgramSessionRepository programSessionRepository;
    private final ProgramExerciseTargetRepository targetRepository;
    private final DtoMapper dtoMapper;
    private final EntityManager entityManager;

    public ProgramSessionService(
            WorkoutProgramRepository workoutProgramRepository,
            ProgramSessionRepository programSessionRepository,
            ProgramExerciseTargetRepository targetRepository,
            DtoMapper dtoMapper,
            EntityManager entityManager
    ) {
        this.workoutProgramRepository = workoutProgramRepository;
        this.programSessionRepository = programSessionRepository;
        this.targetRepository = targetRepository;
        this.dtoMapper = dtoMapper;
        this.entityManager = entityManager;
    }

    /**
     * Loads the next uncompleted session from the user's active workout program.
     *
     * @param userId authenticated user identifier
     * @return optional next program session view, empty when no active or pending session exists
     */
    @Transactional(readOnly = true)
    public Optional<ProgramSessionView> loadNextUncompletedSession(UUID userId) {
        log.info("Loading next uncompleted program session for user {}", userId);
        Optional<WorkoutProgram> currentProgram = resolveCurrentProgramForTracking(userId);
        if (currentProgram.isEmpty()) {
            return Optional.empty();
        }
        return programSessionRepository.findFirstByProgram_IdAndCompletedFalseOrderBySequenceNumberAsc(currentProgram.get().getId())
                .map(session -> dtoMapper.toDto(session, targetRepository.findByProgramSession_IdOrderBySortOrderAsc(session.getId())));
    }

    private Optional<WorkoutProgram> resolveCurrentProgramForTracking(UUID userId) {
        Optional<WorkoutProgram> activeProgram = workoutProgramRepository.findFirstByUserIdAndStatus(userId, ProgramStatus.ACTIVE);
        if (activeProgram.isPresent()) {
            return activeProgram;
        }

        Optional<AcceptedProgramActivation> latestActivation = entityManager
                .createQuery(
                        "select a from AcceptedProgramActivation a where a.userId = :userId order by a.activatedAt desc",
                        AcceptedProgramActivation.class)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();

        if (latestActivation.isEmpty()) {
            return Optional.empty();
        }

        UUID activatedProgramId = latestActivation.get().getActivatedProgramId();
        Optional<WorkoutProgram> activatedProgram = workoutProgramRepository.findById(activatedProgramId)
                .filter(program -> userId.equals(program.getUserId()));

        if (activatedProgram.isPresent()) {
            log.info("Falling back to activated onboarding program {} for user {}", activatedProgramId, userId);
        } else {
            log.warn("Activation record found but activated program {} is missing or not owned by user {}", activatedProgramId, userId);
        }

        return activatedProgram;
    }

    /**
     * Marks a program session as completed and closes the parent program when all sessions are done.
     *
     * @param sessionId program session identifier
     * @param userId authenticated user identifier
     * @throws ResourceNotFoundException when the requested program session does not exist
     * @throws ForbiddenException when the program session belongs to a different user
     */
    @Transactional
    public void markProgramSessionCompleted(UUID sessionId, UUID userId) {
        ProgramSession session = programSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Program session not found"));
        if (!session.getProgram().getUserId().equals(userId)) {
            throw new ForbiddenException("Program session does not belong to the authenticated user");
        }
        session.setCompleted(true);
        programSessionRepository.save(session);

        WorkoutProgram program = session.getProgram();
        boolean allCompleted = programSessionRepository.findByProgram_IdOrderBySequenceNumberAsc(program.getId()).stream()
                .allMatch(ProgramSession::isCompleted);
        if (allCompleted) {
            program.setStatus(ProgramStatus.COMPLETED);
            program.setCompletedAt(OffsetDateTime.now());
            workoutProgramRepository.save(program);
        }
        log.info("Marked program session {} completed for user {}", sessionId, userId);
    }

    /**
     * Resolves a program session and enforces authenticated ownership.
     *
     * @param sessionId program session identifier
     * @param userId authenticated user identifier
     * @return owned program session entity
     * @throws ResourceNotFoundException when the requested program session does not exist
     * @throws ForbiddenException when the program session belongs to a different user
     */
    @Transactional(readOnly = true)
    public ProgramSession requireOwnedProgramSession(UUID sessionId, UUID userId) {
        ProgramSession session = programSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Program session not found"));
        if (!session.getProgram().getUserId().equals(userId)) {
            throw new ForbiddenException("Program session does not belong to the authenticated user");
        }
        return session;
    }
}

