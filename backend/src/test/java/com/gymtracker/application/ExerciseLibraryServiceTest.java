package com.gymtracker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.api.dto.ExerciseDto;
import com.gymtracker.api.exception.ResourceNotFoundException;
import com.gymtracker.domain.Exercise;
import com.gymtracker.domain.ExerciseType;
import com.gymtracker.infrastructure.mapper.DtoMapper;
import com.gymtracker.infrastructure.repository.ExerciseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ExerciseLibraryServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private DtoMapper dtoMapper;

    private ExerciseLibraryService service;

    @BeforeEach
    void setUp() {
        service = new ExerciseLibraryService(exerciseRepository, dtoMapper);
    }

    @Test
    void searchExerciseLibraryReturnsActiveMatchingExercisesCaseInsensitive() {
        Exercise bench = Exercise.builder()
                .id(UUID.randomUUID())
                .name("Bench Press")
                .category("Chest")
                .type(ExerciseType.STRENGTH)
                .active(true)
                .build();
        ExerciseDto benchDto = new ExerciseDto(bench.getId(), bench.getName(), bench.getCategory(), bench.getType(), null);

        when(exerciseRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("bench"))
                .thenReturn(List.of(bench));
        when(dtoMapper.toExerciseDto(bench)).thenReturn(benchDto);

        List<ExerciseDto> results = service.searchExerciseLibrary("bench");

        assertThat(results).containsExactly(benchDto);
        verify(exerciseRepository).findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("bench");
    }

    @Test
    void getTopExercisesReturnsMostUsedExercisesForSuggestions() {
        Exercise bench = Exercise.builder().id(UUID.randomUUID()).name("Bench Press").category("Chest").type(ExerciseType.STRENGTH).active(true).build();
        Exercise deadlift = Exercise.builder().id(UUID.randomUUID()).name("Deadlift").category("Back").type(ExerciseType.STRENGTH).active(true).build();
        ExerciseDto benchDto = new ExerciseDto(bench.getId(), bench.getName(), bench.getCategory(), bench.getType(), null);
        ExerciseDto deadliftDto = new ExerciseDto(deadlift.getId(), deadlift.getName(), deadlift.getCategory(), deadlift.getType(), null);

        when(exerciseRepository.findTopActiveExercisesByUsage(PageRequest.of(0, 50))).thenReturn(List.of(bench, deadlift));
        when(dtoMapper.toExerciseDto(bench)).thenReturn(benchDto);
        when(dtoMapper.toExerciseDto(deadlift)).thenReturn(deadliftDto);

        List<ExerciseDto> results = service.getTopExercises();

        assertThat(results).containsExactly(benchDto, deadliftDto);
    }

    @Test
    void getExerciseByIdReturnsExerciseWhenExists() {
        UUID id = UUID.randomUUID();
        Exercise exercise = Exercise.builder().id(id).name("Bench Press").category("Chest").type(ExerciseType.STRENGTH).active(true).build();
        when(exerciseRepository.findById(id)).thenReturn(Optional.of(exercise));

        Exercise result = service.getExerciseById(id);

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getExerciseByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(exerciseRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExerciseById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Exercise not found");
    }
}

