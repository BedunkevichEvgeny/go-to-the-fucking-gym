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
@Table(name = "cardio_laps")
public class CardioLap {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_entry_id", nullable = false)
    private ExerciseEntry exerciseEntry;

    @Column(name = "lap_order", nullable = false)
    private int lapOrder;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "distance_value", precision = 10, scale = 2)
    private BigDecimal distanceValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_unit", length = 10)
    private DistanceUnit distanceUnit;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

