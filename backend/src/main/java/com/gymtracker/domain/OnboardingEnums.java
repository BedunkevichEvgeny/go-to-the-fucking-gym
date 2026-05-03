package com.gymtracker.domain;

public final class OnboardingEnums {

    private OnboardingEnums() {
    }

    public enum OnboardingPrimaryGoal {
        LOSE_WEIGHT,
        BUILD_HEALTHY_BODY,
        STRENGTH,
        BUILD_MUSCLES
    }

    public enum GoalTargetBucket {
        LOSS_5,
        LOSS_10,
        LOSS_15,
        LOSS_20_PLUS
    }

    public enum OnboardingAttemptStatus {
        IN_PROGRESS,
        ACCEPTED,
        ABANDONED
    }

    public enum ProposalStatus {
        PROPOSED,
        REJECTED,
        ACCEPTED
    }

    public enum ProposalProvider {
        AZURE_OPENAI
    }
}

