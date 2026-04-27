package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.ProgramSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramSessionRepository extends JpaRepository<ProgramSession, UUID> {

    Optional<ProgramSession> findFirstByProgram_IdAndCompletedFalseOrderBySequenceNumberAsc(UUID programId);

    List<ProgramSession> findByProgram_IdOrderBySequenceNumberAsc(UUID programId);
}