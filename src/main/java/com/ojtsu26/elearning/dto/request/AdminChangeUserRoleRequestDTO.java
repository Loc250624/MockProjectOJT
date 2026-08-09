package com.ojtsu26.elearning.dto.request;

import com.ojtsu26.elearning.model.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminChangeUserRoleRequestDTO {
    @NotNull
    private Role role;
}
