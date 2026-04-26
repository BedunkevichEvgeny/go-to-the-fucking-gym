package com.gymtracker.application;

import com.gymtracker.api.dto.ExerciseDto;
import com.gymtracker.api.exception.ResourceNotFoundException;
import com.gymtracker.domain.Exercise;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.ExerciseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseLibraryService {

    private final ExerciseRepository exerciseRepository;
    private final DtoMapper dtoMapper;

    public ExerciseLibraryService(ExerciseRepository exerciseRepository, DtoMapper dtoMapper) {
        this.exerciseRepository = exerciseRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable("exercise-search")
    public List<ExerciseDto> searchExerciseLibrary(String query) {
        return exerciseRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(query == null ? "" : query)
                .stream()
                .map(dtoMapper::toExerciseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getTopExercises() {
        return exerciseRepository.findTop50ByActiveTrueOrderByNameAsc()
                .stream()
                .map(dtoMapper::toExerciseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Exercise getExerciseById(UUID id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found"));
    }
}

