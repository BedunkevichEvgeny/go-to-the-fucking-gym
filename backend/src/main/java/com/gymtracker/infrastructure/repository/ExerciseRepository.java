package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.Exercise;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    Optional<Exercise> findByNameIgnoreCase(String name);

    List<Exercise> findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    @Query("""
            select e
            from Exercise e
            left join ExerciseEntry ee on ee.exerciseId = e.id
            where e.active = true
            group by e.id, e.name, e.category, e.type, e.description, e.active
            order by count(ee.id) desc, e.name asc
            """)
    List<Exercise> findTopActiveExercisesByUsage(Pageable pageable);
}

