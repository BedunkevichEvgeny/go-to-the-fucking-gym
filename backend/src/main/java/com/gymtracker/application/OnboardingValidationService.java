package com.gymtracker.application;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import org.springframework.stereotype.Service;

@Service
public class OnboardingValidationService {

    public void validate(OnboardingSubmissionRequest request) {
        if (request.age() < 13 || request.age() > 100) {
            throw new ValidationException("Age must be between 13 and 100");
        }
        if (request.currentWeight() == null || request.currentWeight().doubleValue() <= 0) {
            throw new ValidationException("Current weight must be greater than zero");
        }
        if (request.primaryGoal() == OnboardingPrimaryGoal.LOSE_WEIGHT && request.goalTargetBucket() == null) {
            throw new ValidationException("goalTargetBucket is required when primaryGoal is LOSE_WEIGHT");
        }
        if (request.primaryGoal() != OnboardingPrimaryGoal.LOSE_WEIGHT && request.goalTargetBucket() != null) {
            throw new ValidationException("goalTargetBucket is only allowed for LOSE_WEIGHT goal");
        }
    }
}

