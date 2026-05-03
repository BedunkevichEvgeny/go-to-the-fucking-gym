package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymtracker.api.dto.ProfileGoalOnboardingDtos.OnboardingSubmissionRequest;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.OnboardingEnums.GoalTargetBucket;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import com.gymtracker.domain.WeightUnit;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OnboardingValidationTest {

    private final OnboardingValidationService service = new OnboardingValidationService();

    @Test
    void acceptsValidLoseWeightSubmissionWithTargetBucket() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                28,
                new BigDecimal("80.5"),
                WeightUnit.KG,
                OnboardingPrimaryGoal.LOSE_WEIGHT,
                GoalTargetBucket.LOSS_10);

        assertThatCode(() -> service.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsLoseWeightSubmissionWithoutTargetBucket() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                28,
                new BigDecimal("80.5"),
                WeightUnit.KG,
                OnboardingPrimaryGoal.LOSE_WEIGHT,
                null);

        assertThatThrownBy(() -> service.validate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("goalTargetBucket");
    }

    @Test
    void rejectsTargetBucketForNonWeightLossGoal() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                28,
                new BigDecimal("80.5"),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                GoalTargetBucket.LOSS_5);

        assertThatThrownBy(() -> service.validate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("only allowed");
    }

    @Test
    void rejectsOutOfRangeAge() {
        OnboardingSubmissionRequest request = new OnboardingSubmissionRequest(
                11,
                new BigDecimal("80.5"),
                WeightUnit.KG,
                OnboardingPrimaryGoal.STRENGTH,
                null);

        assertThatThrownBy(() -> service.validate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Age");
    }
}

