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

    @Transactional(readOnly = true)
    public List<ExerciseView> getTopExercises() {
        return exerciseRepository.findTopActiveExercisesByUsage(
                        PageRequest.of(0, TOP_EXERCISE_LIMIT))
                .stream()
                .map(dtoMapper::toExerciseView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Exercise getExerciseById(UUID id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found"));
    }
}

