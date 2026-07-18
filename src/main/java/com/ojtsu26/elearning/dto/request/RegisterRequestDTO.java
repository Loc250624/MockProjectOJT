package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "Full name is required")
    @ToString.Exclude
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @ToString.Exclude
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @ToString.Exclude
    private String password;

    @NotBlank(message = "Confirm password is required")
    @ToString.Exclude
    private String confirmPassword;
}
