package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
public class LoginRequestDTO {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @ToString.Exclude
    private String email;

    @NotBlank(message = "Password is required")
    @ToString.Exclude
    private String password;
}
