package com.gymtracker.application;

import com.gymtracker.api.dto.ExerciseView;
import com.gymtracker.api.exception.ResourceNotFoundException;
import com.gymtracker.api.exception.ValidationException;
import com.gymtracker.domain.Exercise;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.ExerciseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseLibraryService {

    private static final int TOP_EXERCISE_LIMIT = 50;
    private static final int MAX_QUERY_LENGTH = 120;

    private final ExerciseRepository exerciseRepository;
    private final DtoMapper dtoMapper;

    public ExerciseLibraryService(ExerciseRepository exerciseRepository, DtoMapper dtoMapper) {
        this.exerciseRepository = exerciseRepository;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Searches active exercises by name using a case-insensitive match.
     *
     * @param query free-text query, empty or null to return the default top subset
     * @return list of matching exercise views ordered by name
     * @throws ValidationException when the query exceeds the supported length limit
     */
    @Transactional(readOnly = true)
    @Cacheable("exercise-search")
    public List<ExerciseView> searchExerciseLibrary(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
            throw new ValidationException("Exercise query must be 120 characters or less");
        }
        return exerciseRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(normalizedQuery)
                .stream()
                .map(dtoMapper::toExerciseView)
                .toList();
    }

    /**
     * Returns the most frequently used active exercises for quick selection UIs.
     *
     * @return up to 50 exercise views sorted by usage and name
     */
    @Transactional(readOnly = true)
    public List<ExerciseView> getTopExercises() {
        return exerciseRepository.findTopActiveExercisesByUsage(
                        PageRequest.of(0, TOP_EXERCISE_LIMIT))
                .stream()
                .map(dtoMapper::toExerciseView)
                .toList();
    }

    /**
     * Loads a single exercise entity by identifier.
     *
     * @param id exercise identifier
     * @return persisted exercise entity
     * @throws ResourceNotFoundException when no exercise exists for the provided id
     */
    @Transactional(readOnly = true)
    public Exercise getExerciseById(UUID id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found"));
    }
}

