package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.Exercise;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    Optional<Exercise> findByNameIgnoreCase(String name);

    List<Exercise> findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<Exercise> findTop50ByActiveTrueOrderByNameAsc();
}

