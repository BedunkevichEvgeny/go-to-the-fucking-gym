package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.LoggedSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoggedSessionRepository extends JpaRepository<LoggedSession, UUID>, JpaSpecificationExecutor<LoggedSession> {

    @EntityGraph(attributePaths = {"exerciseEntries", "exerciseEntries.strengthSets", "exerciseEntries.cardioLaps", "feelings"})
    Optional<LoggedSession> findDetailedById(UUID id);
}

