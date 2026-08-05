package com.ojtsu26.elearning.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Required-field annotations own null/blank reporting and avoid duplicate messages.
        return value == null || value.isBlank() || PasswordPolicy.isStrong(value);
    }
}
