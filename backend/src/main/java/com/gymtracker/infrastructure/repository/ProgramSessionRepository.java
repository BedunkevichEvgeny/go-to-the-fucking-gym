package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.ProgramSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramSessionRepository extends JpaRepository<ProgramSession, UUID> {

    Optional<ProgramSession> findFirstByProgramIdAndCompletedFalseOrderBySequenceNumberAsc(UUID programId);

    List<ProgramSession> findByProgramIdOrderBySequenceNumberAsc(UUID programId);
}

