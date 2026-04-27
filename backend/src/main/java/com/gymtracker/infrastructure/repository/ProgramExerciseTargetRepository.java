package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.ProgramExerciseTarget;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramExerciseTargetRepository extends JpaRepository<ProgramExerciseTarget, UUID> {

    List<ProgramExerciseTarget> findByProgramSession_IdOrderBySortOrderAsc(UUID programSessionId);
}


