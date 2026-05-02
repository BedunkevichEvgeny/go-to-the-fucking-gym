package com.gymtracker.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.api.exception.UnauthorizedException;
import com.gymtracker.application.ProgramSessionService;
import com.gymtracker.application.SessionDetailService;
import com.gymtracker.application.security.AuthenticationService;
import com.gymtracker.domain.LoggedSession;
import com.gymtracker.domain.ProgramSession;
import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.SessionType;
import com.gymtracker.domain.WorkoutProgram;
import com.gymtracker.infrastructure.config.SecurityUsersProperties;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.LoggedSessionRepository;
import com.gymtracker.infrastructure.repository.ProgramExerciseTargetRepository;
import com.gymtracker.infrastructure.repository.ProgramSessionRepository;
import com.gymtracker.infrastructure.repository.WorkoutProgramRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserIsolationTest {

    @Mock
    private LoggedSessionRepository loggedSessionRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private WorkoutProgramRepository workoutProgramRepository;

    @Mock
    private ProgramSessionRepository programSessionRepository;

    @Mock
    private ProgramExerciseTargetRepository programExerciseTargetRepository;

    @Mock
    private EntityManager entityManager;

    private SessionDetailService sessionDetailService;
    private ProgramSessionService programSessionService;

    @BeforeEach
    void setUp() {
        sessionDetailService = new SessionDetailService(loggedSessionRepository, dtoMapper);
        programSessionService = new ProgramSessionService(
                workoutProgramRepository,
                programSessionRepository,
                programExerciseTargetRepository,
                new DtoMapper(),
                entityManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userCannotAccessAnotherUsersLoggedSessionDetails() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LoggedSession foreignSession = LoggedSession.builder()
                .id(sessionId)
                .userId(user2)
                .sessionType(SessionType.FREE)
                .sessionDate(LocalDate.of(2026, 4, 27))
                .exerciseEntries(List.of())
                .build();

        when(loggedSessionRepository.findDetailedById(sessionId)).thenReturn(Optional.of(foreignSession));

        assertThatThrownBy(() -> sessionDetailService.getSessionDetails(user1, sessionId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void userCannotUpdateAnotherUsersProgramSessionStatus() {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        WorkoutProgram program = WorkoutProgram.builder()
                .id(UUID.randomUUID())
                .userId(owner)
                .name("Owner Program")
                .status(ProgramStatus.ACTIVE)
                .build();
        ProgramSession session = ProgramSession.builder()
                .id(UUID.randomUUID())
                .program(program)
                .sequenceNumber(1)
                .name("Session 1")
                .completed(false)
                .build();

        when(programSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> programSessionService.markProgramSessionCompleted(session.getId(), attacker))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void authenticationContextIsExtractedAndValidatedForDataAccess() {
        AuthenticationService authenticationService = new AuthenticationService(new SecurityUsersProperties());

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user1", "password1"));
        assertThat(authenticationService.getCurrentUserId())
                .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("unknown-user", "password"));
        assertThatThrownBy(authenticationService::getCurrentUserId)
                .isInstanceOf(UnauthorizedException.class);
    }
}
