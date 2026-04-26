package com.gymtracker.infrastructure.validation;

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
@Constraint(validatedBy = ValidRating.Validator.class)
public @interface ValidRating {

    String message() default "rating must be between 1 and 10";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidRating, Integer> {
        @Override
        public boolean isValid(Integer value, ConstraintValidatorContext context) {
            return value != null && value >= 1 && value <= 10;
        }
    }
}

