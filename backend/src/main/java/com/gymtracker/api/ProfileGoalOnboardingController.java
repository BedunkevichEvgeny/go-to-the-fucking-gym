package com.gymtracker.api;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingAttemptResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposalAcceptanceResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.ProposalRejectRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.TrackingAccessGateResponse;
import com.gymtracker.application.AcceptedProgramActivationService;
import com.gymtracker.application.PlanProposalService;
import com.gymtracker.application.ProposalRevisionService;
import com.gymtracker.application.security.AuthenticationService;
import com.gymtracker.domain.WorkoutProgram;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile-goals")
public class ProfileGoalOnboardingController extends BaseController {

    private final PlanProposalService planProposalService;
    private final ProposalRevisionService proposalRevisionService;
    private final AcceptedProgramActivationService acceptedProgramActivationService;

    public ProfileGoalOnboardingController(
            AuthenticationService authenticationService,
            PlanProposalService planProposalService,
            ProposalRevisionService proposalRevisionService,
            AcceptedProgramActivationService acceptedProgramActivationService
    ) {
        super(authenticationService);
        this.planProposalService = planProposalService;
        this.proposalRevisionService = proposalRevisionService;
        this.acceptedProgramActivationService = acceptedProgramActivationService;
    }

    @PostMapping("/onboarding")
    public PlanProposalResponse createInitialProposal(@Valid @RequestBody OnboardingSubmissionRequest request) {
        UUID userId = extractUserId();
        return planProposalService.createInitialProposal(userId, request);
    }

    @GetMapping("/onboarding/current")
    public ResponseEntity<OnboardingAttemptResponse> getCurrentAttempt() {
        return planProposalService.getCurrentAttempt(extractUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/access-gate")
    public TrackingAccessGateResponse checkTrackingAccessGate() {
        return planProposalService.getTrackingAccessGate(extractUserId());
    }

    @PostMapping("/proposals/{proposalId}/reject")
    public PlanProposalResponse rejectProposal(
            @PathVariable UUID proposalId,
            @Valid @RequestBody ProposalRejectRequest request
    ) {
        return proposalRevisionService.rejectAndRevise(extractUserId(), proposalId, request);
    }

    @PostMapping("/proposals/{proposalId}/accept")
    public ProposalAcceptanceResponse acceptProposal(@PathVariable UUID proposalId) {
        UUID userId = extractUserId();
        // This delegates to acceptance service which handles transaction, authorization, etc.
        WorkoutProgram activatedProgram = acceptedProgramActivationService.acceptProposal(userId, proposalId);

        return new ProposalAcceptanceResponse(
                proposalId,
                activatedProgram.getId(),
                null, // No prior program ID captured in this MVP version
                activatedProgram.getCreatedAt()
        );
    }
}
