package com.ojtsu26.elearning.dto.request;

import com.ojtsu26.elearning.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
public class ChangePasswordRequestDTO {

    @NotBlank(message = "Current password is required")
    @ToString.Exclude
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @StrongPassword
    @ToString.Exclude
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @ToString.Exclude
    private String confirmPassword;
}
