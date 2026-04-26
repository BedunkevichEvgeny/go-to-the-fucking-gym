package com.gymtracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "program_exercise_targets")
public class ProgramExerciseTarget {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_session_id", nullable = false)
    private ProgramSession programSession;

    @Column(name = "exercise_name", nullable = false, length = 120)
    private String exerciseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 20)
    private ExerciseType exerciseType;

    @Column(name = "target_sets")
    private Integer targetSets;

    @Column(name = "target_reps")
    private Integer targetReps;

    @Column(name = "target_weight", precision = 10, scale = 2)
    private BigDecimal targetWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_weight_unit", length = 10)
    private WeightUnit targetWeightUnit;

    @Column(name = "target_duration_seconds")
    private Integer targetDurationSeconds;

    @Column(name = "target_distance", precision = 10, scale = 2)
    private BigDecimal targetDistance;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_distance_unit", length = 10)
    private DistanceUnit targetDistanceUnit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

