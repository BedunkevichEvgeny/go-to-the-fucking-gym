package com.gymtracker.api;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingAttemptResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.PlanProposalResponse;
import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.TrackingAccessGateResponse;
import com.gymtracker.application.PlanProposalService;
import com.gymtracker.application.security.AuthenticationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile-goals")
public class ProfileGoalOnboardingController extends BaseController {

    private final PlanProposalService planProposalService;

    public ProfileGoalOnboardingController(
            AuthenticationService authenticationService,
            PlanProposalService planProposalService
    ) {
        super(authenticationService);
        this.planProposalService = planProposalService;
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
}

