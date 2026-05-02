package com.gymtracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "accepted_program_activations")
public class AcceptedProgramActivation {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "attempt_id", nullable = false, unique = true)
    private UUID attemptId;

    @Column(name = "proposal_id", nullable = false, unique = true)
    private UUID proposalId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activated_program_id", nullable = false)
    private UUID activatedProgramId;

    @Column(name = "replaced_program_id")
    private UUID replacedProgramId;

    @Column(name = "activated_at", nullable = false)
    private OffsetDateTime activatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (activatedAt == null) {
            activatedAt = OffsetDateTime.now();
        }
    }
}

