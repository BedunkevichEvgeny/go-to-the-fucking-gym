package com.gymtracker.infrastructure.validation;

import com.gymtracker.domain.SessionType;
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
@Constraint(validatedBy = ValidSessionType.Validator.class)
public @interface ValidSessionType {

    String message() default "sessionType must be PROGRAM or FREE";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidSessionType, SessionType> {
        @Override
        public boolean isValid(SessionType value, ConstraintValidatorContext context) {
            return value != null;
        }
    }
}

