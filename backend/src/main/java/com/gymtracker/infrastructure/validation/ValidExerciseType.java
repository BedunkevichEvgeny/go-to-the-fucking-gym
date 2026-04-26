package com.gymtracker.infrastructure.validation;

import com.gymtracker.domain.ExerciseType;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidExerciseType.Validator.class)
public @interface ValidExerciseType {

    String message() default "exerciseType must be STRENGTH, BODYWEIGHT, or CARDIO";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidExerciseType, ExerciseType> {
        @Override
        public boolean isValid(ExerciseType value, ConstraintValidatorContext context) {
            return value != null;
        }
    }
}

