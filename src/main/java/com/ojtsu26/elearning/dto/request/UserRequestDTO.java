package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class UserRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String fullName;

    @jakarta.validation.constraints.NotBlank
    private String email;

    @jakarta.validation.constraints.NotBlank
    private String passwordHash;

    @jakarta.validation.constraints.NotBlank
    private String avatarUrl;

    @jakarta.validation.constraints.NotNull
    private Role role;

    @jakarta.validation.constraints.NotNull
    private AuthProvider authProvider;

    @jakarta.validation.constraints.NotNull
    private UserStatus status;
}
