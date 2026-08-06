package com.ojtsu26.elearning.dto.request;

import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class AdminCreateUserRequestDTO {

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    @ToString.Exclude
    private String password;

    @NotBlank(message = "Confirm password is required")
    @ToString.Exclude
    private String confirmPassword;

    @NotNull(message = "Role is required")
    private Role role;
}
