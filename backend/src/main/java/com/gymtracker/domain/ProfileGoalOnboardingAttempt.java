package com.gymtracker.domain;

import com.gymtracker.domain.OnboardingEnums.GoalTargetBucket;
import com.gymtracker.domain.OnboardingEnums.OnboardingAttemptStatus;
import com.gymtracker.domain.OnboardingEnums.OnboardingPrimaryGoal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "profile_goal_onboarding_attempts")
public class ProfileGoalOnboardingAttempt {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int age;

    @Column(name = "current_weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit", nullable = false, length = 10)
    private WeightUnit weightUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_goal", nullable = false, length = 40)
    private OnboardingPrimaryGoal primaryGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_target_bucket", length = 20)
    private GoalTargetBucket goalTargetBucket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OnboardingAttemptStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = OnboardingAttemptStatus.IN_PROGRESS;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

