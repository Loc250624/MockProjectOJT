package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class UserResponseDTO {
    private Integer id;

    private String fullName;

    private String email;

    private String avatarUrl;

    private Role role;

    private AuthProvider authProvider;

    private UserStatus status;

    private java.time.LocalDateTime createdAt;

    private java.time.LocalDateTime updatedAt;
}
