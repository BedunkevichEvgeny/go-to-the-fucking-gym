package com.gymtracker.infrastructure.ai.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.gymtracker.domain.ExerciseType;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jackson deserializer for {@link ExerciseType} that falls back to {@code STRENGTH}
 * instead of throwing {@link IllegalArgumentException} when an unknown value is encountered
 * (e.g., {@code "FLEXIBILITY"} which is not a valid enum constant).
 */
public class SafeExerciseTypeDeserializer extends StdDeserializer<ExerciseType> {

    private static final Logger log = LoggerFactory.getLogger(SafeExerciseTypeDeserializer.class);

    public SafeExerciseTypeDeserializer() {
        super(ExerciseType.class);
    }

    @Override
    public ExerciseType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        try {
            return ExerciseType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Unknown ExerciseType value '{}' from LLM response — falling back to STRENGTH", value);
            return ExerciseType.STRENGTH;
        }
    }
}

