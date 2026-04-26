package com.gymtracker.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Table(name = "program_sessions")
public class ProgramSession {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private WorkoutProgram program;

    @Min(1)
    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @NotBlank
    @Size(min = 1, max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Builder.Default
    @OneToMany(mappedBy = "programSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramExerciseTarget> exerciseTargets = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}

