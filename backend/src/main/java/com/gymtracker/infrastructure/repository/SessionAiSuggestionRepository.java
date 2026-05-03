package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.SessionAiSuggestion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionAiSuggestionRepository extends JpaRepository<SessionAiSuggestion, UUID> {
}

