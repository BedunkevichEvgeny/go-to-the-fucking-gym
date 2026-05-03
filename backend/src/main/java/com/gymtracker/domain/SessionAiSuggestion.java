package com.gymtracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "session_ai_suggestions")
public class SessionAiSuggestion {

    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "session_id")
    private LoggedSession session;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @PrePersist
    void prePersist() {
        if (generatedAt == null) {
            generatedAt = OffsetDateTime.now();
        }
    }
}

