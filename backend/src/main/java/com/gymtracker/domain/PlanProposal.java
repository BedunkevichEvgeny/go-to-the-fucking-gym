package com.gymtracker.domain;

import com.gymtracker.domain.OnboardingEnums.ProposalProvider;
import com.gymtracker.domain.OnboardingEnums.ProposalStatus;
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
@Table(name = "plan_proposals")
public class PlanProposal {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ProfileGoalOnboardingAttempt attempt;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status;

    // Stored as JSON text to keep DB portability while preserving structured payload semantics.
    @Column(name = "proposal_payload", nullable = false, columnDefinition = "text")
    private String proposalPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProposalProvider provider;

    @Column(name = "model_deployment", nullable = false, length = 120)
    private String modelDeployment;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = ProposalStatus.PROPOSED;
        }
        if (provider == null) {
            provider = ProposalProvider.AZURE_OPENAI;
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}

