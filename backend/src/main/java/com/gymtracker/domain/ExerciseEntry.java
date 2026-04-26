package com.gymtracker.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "exercise_entries")
public class ExerciseEntry {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_session_id", nullable = false)
    private LoggedSession loggedSession;

    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(name = "custom_exercise_name", length = 120)
    private String customExerciseName;

    @Column(name = "exercise_name_snapshot", nullable = false, length = 120)
    private String exerciseNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 20)
    private ExerciseType exerciseType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder.Default
    @OneToMany(mappedBy = "exerciseEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StrengthSet> strengthSets = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "exerciseEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardioLap> cardioLaps = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

