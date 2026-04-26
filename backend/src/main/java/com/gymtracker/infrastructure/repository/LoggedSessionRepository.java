package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.LoggedSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface LoggedSessionRepository extends JpaRepository<LoggedSession, UUID>, JpaSpecificationExecutor<LoggedSession> {

    @Query("select ls from LoggedSession ls where ls.id = :id")
    Optional<LoggedSession> findDetailedById(@Param("id") UUID id);
}


