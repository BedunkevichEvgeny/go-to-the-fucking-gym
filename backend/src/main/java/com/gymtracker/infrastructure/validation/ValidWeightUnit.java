package com.gymtracker.infrastructure.validation;

import com.gymtracker.domain.WeightUnit;
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
@Constraint(validatedBy = ValidWeightUnit.Validator.class)
public @interface ValidWeightUnit {

    String message() default "weightUnit must be KG or LBS";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidWeightUnit, WeightUnit> {
        @Override
        public boolean isValid(WeightUnit value, ConstraintValidatorContext context) {
            return value == null || value == WeightUnit.KG || value == WeightUnit.LBS;
        }
    }
}

