package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.ProgramStatus;
import com.gymtracker.domain.WorkoutProgram;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutProgramRepository extends JpaRepository<WorkoutProgram, UUID> {

    Optional<WorkoutProgram> findFirstByUserIdAndStatus(UUID userId, ProgramStatus status);
}

