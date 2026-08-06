package com.ojtsu26.elearning.validation;

import com.ojtsu26.elearning.dto.request.OAuth2CompleteRegistrationRequestDTO;
import com.ojtsu26.elearning.dto.request.RegisterRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationPasswordValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void localRegistrationEnforcesStrongPasswordPolicy() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setFullName("New Student");
        request.setEmail("new.student@example.com");
        request.setPassword("weakpass");
        request.setConfirmPassword("weakpass");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));

        request.setPassword("Strong@123");
        request.setConfirmPassword("Strong@123");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void oauthRegistrationCompletionEnforcesStrongPasswordPolicy() {
        OAuth2CompleteRegistrationRequestDTO request = new OAuth2CompleteRegistrationRequestDTO();
        request.setPassword("NoSpecial123");
        request.setConfirmPassword("NoSpecial123");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));

        request.setPassword("Strong@123");
        request.setConfirmPassword("Strong@123");
        assertThat(validator.validate(request)).isEmpty();
    }
}
