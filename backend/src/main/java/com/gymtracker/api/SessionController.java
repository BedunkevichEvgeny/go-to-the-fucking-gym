package com.gymtracker.api;

import com.gymtracker.api.dto.ExerciseView;
import com.gymtracker.api.dto.LoggedSessionCreateRequest;
import com.gymtracker.api.dto.LoggedSessionDetail;
import com.gymtracker.api.dto.ProgramSessionView;
import com.gymtracker.api.dto.ProgressionResponse;
import com.gymtracker.api.dto.SessionHistoryPage;
import com.gymtracker.api.exception.ForbiddenException;
import com.gymtracker.application.ExerciseLibraryService;
import com.gymtracker.application.FreeSessionService;
import com.gymtracker.application.LoggedSessionService;
import com.gymtracker.application.PlanProposalService;
import com.gymtracker.application.ProgramSessionService;
import com.gymtracker.application.ProgressionService;
import com.gymtracker.application.SessionDetailService;
import com.gymtracker.application.SessionHistoryService;
import com.gymtracker.application.security.AuthenticationService;
import com.gymtracker.domain.SessionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api")
@Validated
public class SessionController extends BaseController {

    private final ProgramSessionService programSessionService;
    private final LoggedSessionService loggedSessionService;
    private final FreeSessionService freeSessionService;
    private final SessionHistoryService sessionHistoryService;
    private final SessionDetailService sessionDetailService;
    private final ExerciseLibraryService exerciseLibraryService;
    private final ProgressionService progressionService;
    private final PlanProposalService planProposalService;

    public SessionController(
            AuthenticationService authenticationService,
            ProgramSessionService programSessionService,
            LoggedSessionService loggedSessionService,
            FreeSessionService freeSessionService,
            SessionHistoryService sessionHistoryService,
            SessionDetailService sessionDetailService,
            ExerciseLibraryService exerciseLibraryService,
            ProgressionService progressionService,
            PlanProposalService planProposalService
    ) {
        super(authenticationService);
        this.programSessionService = programSessionService;
        this.loggedSessionService = loggedSessionService;
        this.freeSessionService = freeSessionService;
        this.sessionHistoryService = sessionHistoryService;
        this.sessionDetailService = sessionDetailService;
        this.exerciseLibraryService = exerciseLibraryService;
        this.progressionService = progressionService;
        this.planProposalService = planProposalService;
    }

    /**
     * Gets the next uncompleted program session for the authenticated user.
     *
     * @return 200 with session payload or 204 when no pending program session exists
     */
    @GetMapping("/program-sessions/next")
    public ResponseEntity<ProgramSessionView> getNextProgramSession() {
        UUID userId = extractUserId();
        if (!planProposalService.getTrackingAccessGate(userId).canAccessProgramTracking()) {
            throw new ForbiddenException("Program tracking is blocked until onboarding is accepted");
        }
        log.info("GET next program session for user {}", userId);
        return programSessionService.loadNextUncompletedSession(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Creates a logged workout session in PROGRAM or FREE mode.
     *
     * @param request logged session payload validated from request body
     * @return 201 response body with persisted session details
     */
    @PostMapping("/logged-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public LoggedSessionDetail createLoggedSession(@Valid @RequestBody LoggedSessionCreateRequest request) {
        UUID userId = extractUserId();
        log.info("POST logged session for user {}", userId);
        return request.sessionType() == SessionType.FREE
                ? freeSessionService.saveFreeSession(userId, request)
                : loggedSessionService.saveLoggedSession(userId, request);
    }

    /**
     * Gets paginated workout history filtered by date range and exercise name.
     *
     * @param page zero-based page index
     * @param size requested page size (1-100)
     * @param dateFrom optional inclusive start date
     * @param dateTo optional inclusive end date
     * @param exerciseName optional exercise name filter
     * @return 200 response with history page payload
     */
    @GetMapping({"/logged-sessions", "/logged-sessions/history"})
    public SessionHistoryPage getHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String exerciseName
    ) {
        UUID userId = extractUserId();
        return sessionHistoryService.getSessionHistory(userId, page, size, dateFrom, dateTo, exerciseName);
    }

    /**
     * Gets full details for a single logged session owned by the authenticated user.
     *
     * @param sessionId logged session identifier
     * @return 200 response with detailed session payload
     */
    @GetMapping("/logged-sessions/{sessionId}")
    public LoggedSessionDetail getSession(@PathVariable UUID sessionId) {
        return sessionDetailService.getSessionDetails(extractUserId(), sessionId);
    }

    /**
     * Gets exercise library results for search or quick-pick usage.
     *
     * @param query optional search query; blank returns top exercises
     * @return 200 response with exercise list
     */
    @GetMapping("/exercises")
    public List<ExerciseView> getExercises(@RequestParam(required = false) String query) {
        return (query == null || query.isBlank())
                ? exerciseLibraryService.getTopExercises()
                : exerciseLibraryService.searchExerciseLibrary(query);
    }

    /**
     * Gets progression points for an exercise across the authenticated user's session history.
     *
     * @param exerciseName exercise name path parameter
     * @return 200 response with progression response payload
     */
    @GetMapping("/progression/{exerciseName}")
    public ProgressionResponse getProgression(@PathVariable String exerciseName) {
        return progressionService.getExerciseProgression(extractUserId(), exerciseName);
    }
}

